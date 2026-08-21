package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.ParsedAnimeRelease;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured attributes from a fansub release title. Deliberately its own class, not an
 * extension of {@link ReleaseParser} — see {@link ParsedAnimeRelease}'s own doc comment for why.
 * Source/video-codec/audio-codec vocabulary genuinely is shared with scene releases (x264, WEB, BD,
 * etc. mean the same thing regardless of content type) and is reused from the same token
 * dictionaries via {@link ReleaseToken#match}; only the *structural* grammar — group tag, episode
 * marker, batch ranges — is anime-specific and parsed separately here.
 */
@ApplicationScoped
public class AnimeReleaseParser {

  private static final Pattern GROUP = Pattern.compile("^\\[([^\\[\\]]{1,50})\\]");
  private static final Pattern CRC32 =
      Pattern.compile("\\[([0-9A-Fa-f]{8})\\](?!.*\\[[0-9A-Fa-f]{8}\\])");
  private static final Pattern BATCH_RANGE =
      Pattern.compile("\\((\\d{1,4})\\s*[-~]\\s*(\\d{1,4})\\)");
  private static final Pattern EPISODE =
      Pattern.compile("\\s-\\s*(\\d{1,4})(?:v(\\d))?(?=[\\s\\[(]|$)", Pattern.CASE_INSENSITIVE);
  private static final Pattern DUAL_AUDIO =
      Pattern.compile("\\bdual[\\s-]?audio\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern BATCH_WORD =
      Pattern.compile("\\b(batch|complete)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern RESOLUTION =
      Pattern.compile(
          "\\b(360|480|540|576|720|900|1080|1440|2160|4320)p\\b", Pattern.CASE_INSENSITIVE);

  public ParsedAnimeRelease parse(String rawTitle) {
    String group = extractGroup(rawTitle);
    String afterGroup =
        group != null ? rawTitle.substring(rawTitle.indexOf(']') + 1).trim() : rawTitle;

    String crc32 = extractCrc32(afterGroup);
    Matcher batchMatcher = BATCH_RANGE.matcher(afterGroup);
    boolean isBatchRange = batchMatcher.find();
    boolean batch = isBatchRange || BATCH_WORD.matcher(afterGroup).find();

    Integer episodeNumber = null;
    Integer episodeRangeEnd = null;
    Integer version = null;
    int markerStart = afterGroup.length();

    if (isBatchRange) {
      episodeNumber = Integer.valueOf(batchMatcher.group(1));
      episodeRangeEnd = Integer.valueOf(batchMatcher.group(2));
      markerStart = batchMatcher.start();
    } else {
      Matcher episodeMatcher = EPISODE.matcher(afterGroup);
      if (episodeMatcher.find()) {
        episodeNumber = Integer.valueOf(episodeMatcher.group(1));
        version = episodeMatcher.group(2) != null ? Integer.valueOf(episodeMatcher.group(2)) : null;
        markerStart = episodeMatcher.start();
      }
    }

    String showTitle = extractShowTitle(afterGroup, markerStart);

    return new ParsedAnimeRelease(
        rawTitle,
        group,
        showTitle,
        episodeNumber,
        episodeRangeEnd,
        version,
        batch,
        DUAL_AUDIO.matcher(afterGroup).find(),
        extractResolution(afterGroup),
        ReleaseToken.match(Source.values(), afterGroup),
        ReleaseToken.match(VideoCodec.values(), afterGroup),
        ReleaseToken.match(AudioCodec.values(), afterGroup),
        crc32);
  }

  private String extractGroup(String rawTitle) {
    Matcher matcher = GROUP.matcher(rawTitle);
    return matcher.find() ? matcher.group(1) : null;
  }

  /**
   * Only the final 8-hex bracket counts — an earlier one is far more likely a resolution/other tag.
   */
  private String extractCrc32(String input) {
    Matcher matcher = CRC32.matcher(input);
    return matcher.find() ? matcher.group(1).toLowerCase() : null;
  }

  private String extractResolution(String input) {
    Matcher matcher = RESOLUTION.matcher(input);
    return matcher.find() ? matcher.group(1) + "p" : null;
  }

  /**
   * Everything before the episode/batch marker, with any leading punctuation left over trimmed off.
   */
  private String extractShowTitle(String afterGroup, int markerStart) {
    String candidate = afterGroup.substring(0, Math.min(markerStart, afterGroup.length())).trim();
    return candidate.replaceAll("[\\s\\-–—]+$", "").trim();
  }
}
