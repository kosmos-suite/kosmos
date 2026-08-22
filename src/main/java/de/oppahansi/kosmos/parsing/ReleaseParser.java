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
  // A year in parens ("Movie (2005)") is the conventional release-year position; preferred over a
  // bare year-shaped number that's actually part of the title itself (e.g. "Paris 2054").
  private static final Pattern PARENTHESIZED_YEAR = Pattern.compile("\\((19\\d{2}|20\\d{2})\\)");

  // A single bracketed tag at the very start ("[XCT] Movie...", "[阿维达] Movie...") — an older
  // P2P/DDL convention distinct from anime's own fansub-bracket grammar (AnimeReleaseParser's own,
  // more elaborate concern); stripped here only when it's a lone leading group, not anywhere else a
  // release title happens to use brackets.
  private static final Pattern LEADING_BRACKET_TAG = Pattern.compile("^\\[[^\\]]{1,30}\\]\\s*");

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

  // Radarr's "Quality Modifier: Remux" — a distinct token from Source (see ParsedRelease#remux).
  private static final Pattern REMUX = Pattern.compile("\\bremux\\b", Pattern.CASE_INSENSITIVE);

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
  // The optional [\s\-x]? tolerates "S06xE01" and hyphen-joined "S03-E01" forms alongside the
  // standard adjacent "S01E05" one — still anchored tightly enough by the S…E…digits shape not to
  // false-positive on ordinary text.
  private static final Pattern SEASON_EPISODE =
      Pattern.compile(
          "\\bS(\\d{1,2})[\\s\\-x]?E(\\d{1,3})(?:E\\d{1,3})*\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEASON_EPISODE_X =
      Pattern.compile("\\b(\\d{1,2})x(\\d{2,3})\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEASON_PACK =
      Pattern.compile("\\bS(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);

  public ParsedRelease parse(String rawTitle) {
    String releaseGroup = extractReleaseGroup(rawTitle);
    String normalized = rawTitle.replace('.', ' ').replace('_', ' ').trim();

    Integer year = extractYear(normalized);
    String resolution = extractResolution(normalized);
    String source = ReleaseToken.match(Source.values(), normalized);
    String videoCodec = ReleaseToken.match(VideoCodec.values(), normalized);
    String audioCodec = ReleaseToken.match(AudioCodec.values(), normalized);
    String edition = ReleaseToken.match(Edition.values(), normalized);
    boolean proper = PROPER.matcher(normalized).find();
    boolean repack = REPACK.matcher(normalized).find();
    boolean remux = REMUX.matcher(normalized).find();
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
        remux,
        seasonEpisode[0],
        seasonEpisode[1]);
  }

  // Trailing separator punctuation a cut can leave dangling — "Dark City (" once "(1998)" is cut
  // at its opening paren, "Baraka_Edition_Collector" once "Edition Collector" is cut leaving
  // "Baraka_", etc. Applied after every cut, not baked into any one marker pattern, since any of
  // them can leave this behind.
  private static final Pattern TRAILING_SEPARATORS = Pattern.compile("[\\s\\-(\\[,]+$");

  /**
   * Everything before whichever comes first: a season/episode marker, a year, or a
   * resolution/source marker — the release title's own technical tags, which bound the real title
   * even when no year is present. Falls back to the whole (separator-normalized) input when none of
   * these are present, same as a title with no other markers at all would leave nothing to cut.
   */
  private String extractCleanTitle(String normalized) {
    normalized = stripLeadingTagIfNotTechnical(normalized);
    int cutAt = normalized.length();
    for (Pattern marker : new Pattern[] {SEASON_EPISODE, SEASON_EPISODE_X, SEASON_PACK}) {
      Matcher matcher = marker.matcher(normalized);
      if (matcher.find() && matcher.start() < cutAt) {
        cutAt = matcher.start();
      }
    }
    // Same parenthesized-preferred position #extractYear uses for the value — a title can itself
    // contain a year-shaped number ("Paris 2054, Renaissance (2005)"), so cutting at the first bare
    // match would truncate the real title too early.
    int yearStart = yearCutPosition(normalized);
    if (yearStart != -1 && yearStart < cutAt) {
      cutAt = yearStart;
    }
    int resolutionStart = matcherStart(RESOLUTION, normalized);
    if (resolutionStart != -1 && resolutionStart < cutAt) {
      cutAt = resolutionStart;
    }
    int sourceStart = ReleaseToken.earliestMatchStart(Source.values(), normalized);
    if (sourceStart != -1 && sourceStart < cutAt) {
      cutAt = sourceStart;
    }
    int editionStart = ReleaseToken.earliestMatchStart(Edition.values(), normalized);
    if (editionStart != -1 && editionStart < cutAt) {
      cutAt = editionStart;
    }
    String cut =
        TRAILING_SEPARATORS.matcher(normalized.substring(0, cutAt).trim()).replaceAll("").trim();
    return cut.isEmpty() ? normalized : cut;
  }

  private int yearCutPosition(String normalized) {
    Matcher parenthesized = PARENTHESIZED_YEAR.matcher(normalized);
    if (parenthesized.find()) {
      return parenthesized.start();
    }
    return matcherStart(YEAR, normalized);
  }

  private int matcherStart(Pattern pattern, String input) {
    Matcher matcher = pattern.matcher(input);
    return matcher.find() ? matcher.start() : -1;
  }

  /**
   * Only for {@link #extractCleanTitle}, never the shared {@code normalized} every other field is
   * read from — a leading bracket ("[XCT] Movie...") is usually a meaningless site/uploader tag,
   * but occasionally carries real technical info ("[h265 - hevc] Movie..."), which must stay
   * visible to resolution/source/codec detection. Stripping it only when it contains none of those
   * avoids losing that signal while still cleaning up the common case.
   */
  private String stripLeadingTagIfNotTechnical(String normalized) {
    Matcher tag = LEADING_BRACKET_TAG.matcher(normalized);
    if (!tag.find()) {
      return normalized;
    }
    String bracketContent = normalized.substring(0, tag.end());
    boolean technical =
        RESOLUTION.matcher(bracketContent).find()
            || ReleaseToken.match(Source.values(), bracketContent) != null
            || ReleaseToken.match(VideoCodec.values(), bracketContent) != null
            || ReleaseToken.match(AudioCodec.values(), bracketContent) != null;
    return technical ? normalized : tag.replaceFirst("");
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

  /**
   * A parenthesized year ("Movie (2005)") wins over a bare year-shaped number even if the bare one
   * comes first — a title can itself contain a year-like number ("Paris 2054, Renaissance (2005)"),
   * and the parenthesized form is the conventional release-year position.
   */
  private Integer extractYear(String normalized) {
    Matcher parenthesized = PARENTHESIZED_YEAR.matcher(normalized);
    if (parenthesized.find()) {
      return Integer.valueOf(parenthesized.group(1));
    }
    return extractGroup(YEAR, normalized).map(Integer::parseInt).orElse(null);
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
