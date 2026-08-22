package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts structured attributes from a release title using proven token dictionaries. */
@ApplicationScoped
public class ReleaseParser {

  private static final Pattern YEAR = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");

  // Progressive/interlaced values ported from guessit's screen_size property config
  // (guessit-io/guessit, LGPL-3.0).
  private static final Pattern RESOLUTION =
      Pattern.compile(
          "\\b(360|480|540|576|720|900|1080|1440|2160|4320)(p|i)\\b", Pattern.CASE_INSENSITIVE);

  // Ported from guessit's "other" property config: Proper matches Proper/Real/Real-Proper/
  // Real-Repack/Real-Rerip; Repack/Rerip are kept as a separate flag here (rather than folded
  // into Proper as guessit does) since quality-scoring rules conventionally treat them as
  // distinct conditions.
  private static final Pattern PROPER =
      Pattern.compile(
          "\\b(real-?proper|real-?repack|real-?rerip|proper|real)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern REPACK =
      Pattern.compile("\\b(repack\\d*|rerip\\d*)\\b", Pattern.CASE_INSENSITIVE);

  // Ported from guessit's release_group property: the group tag is the final hyphen-separated
  // token in a scene-style release title (Movie.2020.1080p.BluRay.x264-GROUP). Anime's bracketed
  // fansub-group convention ([SubsPlease] Show - 01) is a different grammar entirely — deferred
  // to the anime parser module per the roadmap, not handled here.
  private static final Pattern RELEASE_GROUP = Pattern.compile("-([A-Za-z0-9][\\w]{1,29})$");

  // Standard scene SxxExx (Show.S01E05...), the NxNN shorthand (Show.10x05...), and a season-pack
  // fallback (Show.S01.1080p...) with no episode marker at all — tried in that order since S01E05
  // would otherwise also satisfy the season-pack pattern on its "S01" prefix. A release with
  // multiple episodes (S01E05E06) is treated as starting at the first one; Kosmos doesn't model
  // multi-episode releases as a single grab spanning several MediaItems yet.
  private static final Pattern SEASON_EPISODE =
      Pattern.compile("\\bS(\\d{1,2})E(\\d{1,3})(?:E\\d{1,3})*\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEASON_EPISODE_X =
      Pattern.compile("\\b(\\d{1,2})x(\\d{2,3})\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEASON_PACK =
      Pattern.compile("\\bS(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);

  public ParsedRelease parse(String rawTitle) {
    String releaseGroup = extractReleaseGroup(rawTitle);
    String normalized = rawTitle.replace('.', ' ').replace('_', ' ').trim();

    Integer year = extractGroup(YEAR, normalized).map(Integer::parseInt).orElse(null);
    String resolution = extractResolution(normalized);
    String source = ReleaseToken.match(Source.values(), normalized);
    String videoCodec = ReleaseToken.match(VideoCodec.values(), normalized);
    String audioCodec = ReleaseToken.match(AudioCodec.values(), normalized);
    String edition = ReleaseToken.match(Edition.values(), normalized);
    boolean proper = PROPER.matcher(normalized).find();
    boolean repack = REPACK.matcher(normalized).find();
    Integer[] seasonEpisode = extractSeasonEpisode(normalized);
    String cleanTitle = extractCleanTitle(normalized);

    return new ParsedRelease(
        rawTitle,
        cleanTitle,
        year,
        resolution,
        source,
        videoCodec,
        audioCodec,
        edition,
        releaseGroup,
        proper,
        repack,
        seasonEpisode[0],
        seasonEpisode[1]);
  }

  /**
   * Everything before whichever comes first: a season/episode marker, or a year — the two most
   * reliable "the real title ends here" signals a release title carries. Falls back to the whole
   * (separator-normalized) input when neither is present, same as a title with no other markers at
   * all would leave nothing to cut.
   */
  private String extractCleanTitle(String normalized) {
    int cutAt = normalized.length();
    for (Pattern marker : new Pattern[] {SEASON_EPISODE, SEASON_EPISODE_X, SEASON_PACK, YEAR}) {
      Matcher matcher = marker.matcher(normalized);
      if (matcher.find() && matcher.start() < cutAt) {
        cutAt = matcher.start();
      }
    }
    String cut = normalized.substring(0, cutAt).trim();
    return cut.isEmpty() ? normalized : cut;
  }

  /** [seasonNumber, episodeNumber] — either or both may be null; see the patterns' own comment. */
  private Integer[] extractSeasonEpisode(String input) {
    Matcher se = SEASON_EPISODE.matcher(input);
    if (se.find()) {
      return new Integer[] {Integer.valueOf(se.group(1)), Integer.valueOf(se.group(2))};
    }
    Matcher x = SEASON_EPISODE_X.matcher(input);
    if (x.find()) {
      return new Integer[] {Integer.valueOf(x.group(1)), Integer.valueOf(x.group(2))};
    }
    Matcher pack = SEASON_PACK.matcher(input);
    if (pack.find()) {
      return new Integer[] {Integer.valueOf(pack.group(1)), null};
    }
    return new Integer[] {null, null};
  }

  private String extractReleaseGroup(String rawTitle) {
    return extractGroup(RELEASE_GROUP, rawTitle.trim()).orElse(null);
  }

  private String extractResolution(String input) {
    Matcher matcher = RESOLUTION.matcher(input);
    if (!matcher.find()) {
      return null;
    }
    return matcher.group(1) + matcher.group(2).toLowerCase();
  }

  private Optional<String> extractGroup(Pattern pattern, String input) {
    Matcher matcher = pattern.matcher(input);
    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
  }
}
