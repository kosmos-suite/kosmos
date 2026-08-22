package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.library.dto.ImportCandidate;
import de.oppahansi.kosmos.media.Anime;
import de.oppahansi.kosmos.media.AnimeEpisode;
import de.oppahansi.kosmos.media.AnimeSeason;
import de.oppahansi.kosmos.media.Episode;
import de.oppahansi.kosmos.media.Movie;
import de.oppahansi.kosmos.media.Season;
import de.oppahansi.kosmos.media.Show;
import de.oppahansi.kosmos.metadata.TitleMatching;
import de.oppahansi.kosmos.parsing.AnimeReleaseParser;
import de.oppahansi.kosmos.parsing.ReleaseParser;
import de.oppahansi.kosmos.parsing.dto.ParsedAnimeRelease;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Best-effort auto-match for the manual-import review screen: parses a file's own name and looks
 * for a single unambiguous library title it could belong to. Never authoritative — {@link
 * ImportCandidate#ambiguous()} flags anything with more than one plausible match, and a caller is
 * always free to substitute a different {@code suggestedMediaItemId} before committing, the same
 * way a wrong automatic search result is overridable via interactive search.
 *
 * <p>Which parser runs is decided by one cheap, real-world-reliable signal: a leading {@code
 * [Group]} tag means fansub-style anime naming ({@link AnimeReleaseParser}, absolute numbering) —
 * everything else goes through the scene-release parser ({@link ReleaseParser}, {@code SxxExx}). A
 * season/episode match from the scene parser still checks the Anime library as well as Show, since
 * a fair amount of real anime (Crunchyroll/AMZN/BILI simulcasts, most notably) ships under plain
 * {@code SxxExx} naming with no fansub-style tag at all — {@link AnimeSeason#seasonNumber()} and
 * {@link AnimeEpisode#episodeNumber()} (within-season, not the franchise-absolute number) are
 * exactly the fields that join against a scene-style match the same way {@link Season}/{@link
 * Episode} already do for a real Show.
 */
@ApplicationScoped
public class ImportMatchService {

  @Inject ReleaseParser releaseParser;
  @Inject AnimeReleaseParser animeReleaseParser;

  public List<ImportCandidate> scan(String sourcePathRaw) {
    Path source = Path.of(sourcePathRaw);
    return VideoFileScanner.findAll(source).stream().map(this::match).toList();
  }

  private ImportCandidate match(Path file) {
    String filename = stripExtension(file.getFileName().toString());
    long size = sizeOrZero(file);
    String sourcePath = file.toString();

    if (filename.trim().startsWith("[")) {
      return matchAnimeAbsolute(sourcePath, size, filename);
    }

    ParsedRelease parsed = releaseParser.parse(filename);
    if (parsed.seasonNumber() != null && parsed.episodeNumber() != null) {
      return matchSeasonEpisode(sourcePath, size, parsed);
    }
    return matchMovie(sourcePath, size, parsed);
  }

  /**
   * {@link TitleMatching#looselyMatch}'s empty-title handling means "not confidently a mismatch,"
   * meant for a caller that also has a second corroborating signal (year, episode count, ...)
   * before trusting a match — not "confidently a match" on its own. A title {@link
   * TitleMatching#normalize} strips down to nothing (a non-Latin script most commonly — verified
   * live against a real Cyrillic-titled file, which without this guard matched every single movie
   * in the library) has no such signal here, so it's treated as unmatched rather than silently
   * matching everything.
   */
  private boolean hasUsableTitle(String title) {
    return !TitleMatching.normalize(title).isEmpty();
  }

  private ImportCandidate matchMovie(String sourcePath, long size, ParsedRelease parsed) {
    if (!hasUsableTitle(parsed.cleanTitle())) {
      return ImportCandidate.unmatched(sourcePath, size, parsed.cleanTitle(), null, null, null);
    }
    List<Movie> candidates =
        Movie.<Movie>listAll().stream()
            .filter(m -> TitleMatching.looselyMatch(parsed.cleanTitle(), m.mediaItem.title))
            .filter(
                m ->
                    parsed.year() == null
                        || m.mediaItem.year == null
                        || parsed.year().equals(m.mediaItem.year))
            .toList();
    if (candidates.isEmpty()) {
      return ImportCandidate.unmatched(sourcePath, size, parsed.cleanTitle(), null, null, null);
    }
    Movie best = candidates.get(0);
    return new ImportCandidate(
        sourcePath,
        size,
        parsed.cleanTitle(),
        null,
        null,
        null,
        best.mediaItemId,
        best.mediaItem.title,
        "movie",
        candidates.size() > 1);
  }

  private ImportCandidate matchSeasonEpisode(String sourcePath, long size, ParsedRelease parsed) {
    if (!hasUsableTitle(parsed.cleanTitle())) {
      return ImportCandidate.unmatched(
          sourcePath,
          size,
          parsed.cleanTitle(),
          parsed.seasonNumber(),
          parsed.episodeNumber(),
          null);
    }
    Optional<Episode> showMatch = matchShowEpisode(parsed);
    if (showMatch.isPresent()) {
      Episode episode = showMatch.get();
      boolean ambiguous = countShowTitleMatches(parsed.cleanTitle()) > 1;
      return new ImportCandidate(
          sourcePath,
          size,
          parsed.cleanTitle(),
          parsed.seasonNumber(),
          parsed.episodeNumber(),
          null,
          episode.mediaItemId,
          episode.season.show.mediaItem.title,
          "episode",
          ambiguous);
    }
    Optional<AnimeEpisode> animeMatch = matchAnimeEpisodeBySeason(parsed);
    if (animeMatch.isPresent()) {
      AnimeEpisode episode = animeMatch.get();
      boolean ambiguous = countAnimeTitleMatches(parsed.cleanTitle()) > 1;
      return new ImportCandidate(
          sourcePath,
          size,
          parsed.cleanTitle(),
          parsed.seasonNumber(),
          parsed.episodeNumber(),
          episode.absoluteEpisodeNumber,
          episode.mediaItemId,
          episode.season.anime.mediaItem.title,
          "anime_episode",
          ambiguous);
    }
    return ImportCandidate.unmatched(
        sourcePath, size, parsed.cleanTitle(), parsed.seasonNumber(), parsed.episodeNumber(), null);
  }

  private Optional<Episode> matchShowEpisode(ParsedRelease parsed) {
    return Show.<Show>listAll().stream()
        .filter(s -> TitleMatching.looselyMatch(parsed.cleanTitle(), s.mediaItem.title))
        .flatMap(
            s ->
                Season.<Season>find("show = ?1 and seasonNumber = ?2", s, parsed.seasonNumber())
                    .firstResultOptional()
                    .stream())
        .flatMap(
            season ->
                Episode.<Episode>find(
                    "season = ?1 and episodeNumber = ?2", season, parsed.episodeNumber())
                    .firstResultOptional()
                    .stream())
        .findFirst();
  }

  private Optional<AnimeEpisode> matchAnimeEpisodeBySeason(ParsedRelease parsed) {
    return Anime.<Anime>listAll().stream()
        .filter(a -> TitleMatching.looselyMatch(parsed.cleanTitle(), a.mediaItem.title))
        .flatMap(
            a ->
                AnimeSeason.<AnimeSeason>find(
                    "anime = ?1 and seasonNumber = ?2", a, parsed.seasonNumber())
                    .firstResultOptional()
                    .stream())
        .flatMap(
            season ->
                AnimeEpisode.<AnimeEpisode>find(
                    "season = ?1 and episodeNumber = ?2", season, parsed.episodeNumber())
                    .firstResultOptional()
                    .stream())
        .findFirst();
  }

  private ImportCandidate matchAnimeAbsolute(String sourcePath, long size, String filename) {
    ParsedAnimeRelease parsed = animeReleaseParser.parse(filename);
    Integer absolute = parsed.episodeNumber();
    if (parsed.batch() || absolute == null || !hasUsableTitle(parsed.showTitle())) {
      return ImportCandidate.unmatched(sourcePath, size, parsed.showTitle(), null, null, absolute);
    }

    List<Anime> candidates =
        Anime.<Anime>listAll().stream()
            .filter(a -> TitleMatching.looselyMatch(parsed.showTitle(), a.mediaItem.title))
            .toList();
    Optional<AnimeEpisode> episode =
        candidates.stream()
            .flatMap(
                a ->
                    AnimeEpisode.<AnimeEpisode>find(
                        "season.anime = ?1 and absoluteEpisodeNumber = ?2", a, absolute)
                        .firstResultOptional()
                        .stream())
            .findFirst();
    if (episode.isEmpty()) {
      return ImportCandidate.unmatched(sourcePath, size, parsed.showTitle(), null, null, absolute);
    }
    return new ImportCandidate(
        sourcePath,
        size,
        parsed.showTitle(),
        episode.get().season.seasonNumber,
        episode.get().episodeNumber,
        absolute,
        episode.get().mediaItemId,
        episode.get().season.anime.mediaItem.title,
        "anime_episode",
        candidates.size() > 1);
  }

  private long countShowTitleMatches(String title) {
    return Show.<Show>listAll().stream()
        .filter(s -> TitleMatching.looselyMatch(title, s.mediaItem.title))
        .count();
  }

  private long countAnimeTitleMatches(String title) {
    return Anime.<Anime>listAll().stream()
        .filter(a -> TitleMatching.looselyMatch(title, a.mediaItem.title))
        .count();
  }

  private String stripExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    return dot > 0 ? filename.substring(0, dot) : filename;
  }

  private long sizeOrZero(Path path) {
    try {
      return Files.size(path);
    } catch (IOException e) {
      return 0L;
    }
  }
}
