package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.downloads.dto.GrabRequest;
import de.oppahansi.kosmos.indexers.Indexer;
import de.oppahansi.kosmos.indexers.TorznabClient;
import de.oppahansi.kosmos.indexers.dto.TorznabResult;
import de.oppahansi.kosmos.media.MinimumAvailability;
import de.oppahansi.kosmos.media.Movie;
import de.oppahansi.kosmos.parsing.QualityDefinitionService;
import de.oppahansi.kosmos.parsing.ReleaseParser;
import de.oppahansi.kosmos.parsing.ScoringEngine;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import de.oppahansi.kosmos.parsing.dto.ScoredRelease;
import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.ProgressReporter;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The unattended counterpart to interactive search: for every movie with a {@link
 * Movie#qualityProfile} assigned, no {@link Grab} yet, and clear of its {@link MinimumAvailability}
 * gate, searches every enabled {@link Indexer}, scores each result the same way {@code
 * IndexerResource}'s manual search does, and grabs the single highest-scoring release that clears
 * the profile's cutoff — no human review, which is exactly what distinguishes this from interactive
 * search. The availability gate only applies here — a human running interactive search already
 * knows what they're getting.
 *
 * <p>Deliberately simple for a first pass: highest score wins outright (no seeders/size tie-break),
 * and the release goes to the first enabled {@link DownloadClient} — same one-download-client
 * shortcut the frontend's search/grab pages already take, not a new inconsistency. A movie with no
 * quality profile assigned is simply never considered, which is the opt-in signal — there's no
 * separate "monitored" flag yet.
 *
 * <p>A profile with {@code grabDelayMinutes > 0} (Radarr's Delay Profile, simplified — see {@link
 * de.oppahansi.kosmos.parsing.QualityProfile}'s own doc) doesn't grab {@code best} outright; see
 * {@link #clearsDelay}.
 */
@ApplicationScoped
public class AutomaticSearchJob implements JobHandler {

  @Inject TorznabClient torznabClient;
  @Inject ReleaseParser releaseParser;
  @Inject ScoringEngine scoringEngine;
  @Inject QualityDefinitionService qualityDefinitionService;
  @Inject GrabService grabService;
  @Inject BlocklistService blocklistService;

  @Override
  public String jobName() {
    return "automatic-search";
  }

  @Override
  public String displayName() {
    return "Movie Automatic Search";
  }

  @Override
  public int defaultIntervalSeconds() {
    return 21600; // 6 hours
  }

  /**
   * Each movie gets its own fresh transaction, same isolation as {@link DownloadStatusPollJob} —
   * one movie hitting an unreachable indexer or download client can't stop the rest of the library
   * from being searched in this run.
   */
  @Override
  public String run(ProgressReporter progress) {
    List<UUID> eligible = QuarkusTransaction.requiringNew().call(this::findEligibleMovieIds);
    for (UUID movieId : eligible) {
      try {
        QuarkusTransaction.requiringNew().run(() -> searchAndGrab(movieId));
      } catch (RuntimeException e) {
        // Left ungrabbed; retried on the next tick, same as an unimportable DownloadStatusPollJob
        // grab.
      }
    }
    return null;
  }

  private List<UUID> findEligibleMovieIds() {
    LocalDate today = LocalDate.now();
    return Movie.<Movie>list("qualityProfile is not null").stream()
        .filter(
            movie ->
                movie
                    .parsedMinimumAvailability()
                    .isAvailable(movie.releaseDate, movie.digitalReleaseDate, today))
        .map(movie -> movie.mediaItemId)
        .filter(mediaItemId -> !Grab.hasActiveGrab(mediaItemId))
        .toList();
  }

  private void searchAndGrab(UUID movieId) {
    Movie movie = Movie.<Movie>findById(movieId);
    if (movie == null || movie.qualityProfile == null) {
      return;
    }
    List<Indexer> indexers = Indexer.list("enabled", true);
    if (indexers.isEmpty()) {
      return;
    }
    DownloadClient client = DownloadClient.<DownloadClient>find("enabled", true).firstResult();
    if (client == null) {
      return;
    }

    Best best = null;
    for (Indexer indexer : indexers) {
      List<TorznabResult> results;
      try {
        results = torznabClient.search(indexer.baseUrl, indexer.apiKey, movie.mediaItem.title);
      } catch (IOException | InterruptedException e) {
        continue; // this indexer is unreachable this run; the others still get a chance
      }
      for (TorznabResult raw : results) {
        if (blocklistService.isBlocked(movieId, raw.downloadUrl())) {
          continue;
        }
        ParsedRelease parsed = releaseParser.parse(raw.title());
        ScoredRelease scored = scoringEngine.score(parsed, movie.qualityProfile);
        boolean sizeOk =
            qualityDefinitionService.checkSizeGate(parsed, raw.sizeBytes(), movie.runtimeMinutes)
                == null;
        if (scored.passesCutoff()
            && sizeOk
            && (best == null || scored.totalScore() > best.scored.totalScore())) {
          best = new Best(raw, parsed, scored);
        }
      }
    }
    if (best == null) {
      return;
    }
    if (!clearsDelay(movie, best)) {
      return;
    }

    PendingCandidate.delete("mediaItem.id", movieId);
    grabService.grab(
        movieId,
        new GrabRequest(
            best.raw.title(),
            best.raw.downloadUrl(),
            best.parsed.resolution(),
            best.parsed.source(),
            best.parsed.videoCodec(),
            best.scored.totalScore(),
            client.id));
  }

  /**
   * {@code grabDelayMinutes == 0} (every profile's default) always clears immediately — existing
   * behavior, unchanged. Otherwise grabs right away if {@code bypassScore} is met, or if {@code
   * best} is the same release {@link PendingCandidate} has been tracking for at least the delay; a
   * new or first-seen candidate just gets recorded and waits for a later run.
   */
  private boolean clearsDelay(Movie movie, Best best) {
    int delayMinutes = movie.qualityProfile.grabDelayMinutes;
    if (delayMinutes <= 0) {
      return true;
    }
    Integer bypassScore = movie.qualityProfile.bypassScore;
    if (bypassScore != null && best.scored.totalScore() >= bypassScore) {
      return true;
    }

    PendingCandidate pending =
        PendingCandidate.<PendingCandidate>find("mediaItem.id", movie.mediaItemId).firstResult();
    if (pending == null || !pending.downloadUrl.equals(best.raw.downloadUrl())) {
      if (pending == null) {
        pending = new PendingCandidate();
        pending.mediaItem = movie.mediaItem;
        pending.persist();
      }
      pending.downloadUrl = best.raw.downloadUrl();
      pending.firstSeenAt = Instant.now();
      return false;
    }
    return delayElapsed(pending.firstSeenAt, delayMinutes, Instant.now());
  }

  /**
   * Pulled out as a pure function so the boundary (exactly at the delay) is unit-testable without
   * CDI.
   */
  static boolean delayElapsed(Instant firstSeenAt, int delayMinutes, Instant now) {
    return !Duration.between(firstSeenAt, now).minus(Duration.ofMinutes(delayMinutes)).isNegative();
  }

  private record Best(TorznabResult raw, ParsedRelease parsed, ScoredRelease scored) {}
}
