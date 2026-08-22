package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.downloads.dto.GrabRequest;
import de.oppahansi.kosmos.indexers.Indexer;
import de.oppahansi.kosmos.indexers.TorznabClient;
import de.oppahansi.kosmos.indexers.dto.TorznabResult;
import de.oppahansi.kosmos.media.Movie;
import de.oppahansi.kosmos.parsing.QualityDefinitionService;
import de.oppahansi.kosmos.parsing.ReleaseParser;
import de.oppahansi.kosmos.parsing.ScoringEngine;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import de.oppahansi.kosmos.parsing.dto.ScoredRelease;
import de.oppahansi.kosmos.scheduler.JobHandler;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * The unattended counterpart to interactive search: for every movie with a {@link
 * Movie#qualityProfile} assigned and no {@link Grab} yet, searches every enabled {@link Indexer},
 * scores each result the same way {@code IndexerResource}'s manual search does, and grabs the
 * single highest-scoring release that clears the profile's cutoff — no human review, which is
 * exactly what distinguishes this from interactive search.
 *
 * <p>Deliberately simple for a first pass: highest score wins outright (no seeders/size tie-break),
 * and the release goes to the first enabled {@link DownloadClient} — same one-download-client
 * shortcut the frontend's search/grab pages already take, not a new inconsistency. A movie with no
 * quality profile assigned is simply never considered, which is the opt-in signal — there's no
 * separate "monitored" flag yet.
 */
@ApplicationScoped
public class AutomaticSearchJob implements JobHandler {

  @Inject TorznabClient torznabClient;
  @Inject ReleaseParser releaseParser;
  @Inject ScoringEngine scoringEngine;
  @Inject QualityDefinitionService qualityDefinitionService;
  @Inject GrabService grabService;

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
  public String run() {
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
    return Movie.<Movie>list("qualityProfile is not null").stream()
        .map(movie -> movie.mediaItemId)
        .filter(mediaItemId -> Grab.count("release.mediaItem.id = ?1", mediaItemId) == 0)
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

  private record Best(TorznabResult raw, ParsedRelease parsed, ScoredRelease scored) {}
}
