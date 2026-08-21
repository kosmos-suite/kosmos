package de.oppahansi.kosmos.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.oppahansi.kosmos.parsing.dto.ParsedAnimeRelease;
import org.junit.jupiter.api.Test;

class AnimeReleaseParserTest {

  private final AnimeReleaseParser parser = new AnimeReleaseParser();

  @Test
  void parsesStandardSingleEpisodeRelease() {
    ParsedAnimeRelease result =
        parser.parse("[SubsPlease] Chainsaw Man - 01 (1080p) [12AB34CD].mkv");

    assertEquals("SubsPlease", result.releaseGroup());
    assertEquals("Chainsaw Man", result.showTitle());
    assertEquals(1, result.episodeNumber());
    assertNull(result.episodeRangeEnd());
    assertNull(result.version());
    assertFalse(result.batch());
    assertFalse(result.dualAudio());
    assertEquals("1080p", result.resolution());
    assertEquals("12ab34cd", result.crc32());
  }

  @Test
  void parsesGroupNameContainingAHyphen() {
    ParsedAnimeRelease result =
        parser.parse("[Erai-raws] Spy x Family - 25 [1080p][Multiple Subtitle][A1B2C3D4].mkv");

    assertEquals("Erai-raws", result.releaseGroup());
    assertEquals("Spy x Family", result.showTitle());
    assertEquals(25, result.episodeNumber());
    assertEquals("a1b2c3d4", result.crc32());
    assertEquals("1080p", result.resolution());
  }

  @Test
  void parsesBatchRangeAsEpisodeRangeNotASingleEpisode() {
    ParsedAnimeRelease result =
        parser.parse(
            "[Judas] One Piece (001-1122) (Season 1-21) [1080p][HEVC 10bit x265][Dual-Audio]");

    assertEquals("Judas", result.releaseGroup());
    assertEquals("One Piece", result.showTitle());
    assertTrue(result.batch());
    assertEquals(1, result.episodeNumber());
    assertEquals(1122, result.episodeRangeEnd());
    assertTrue(result.dualAudio());
    assertEquals("H.265", result.videoCodec());
  }

  @Test
  void parsesEpisodeVersionSuffix() {
    ParsedAnimeRelease result =
        parser.parse("[Anime Time] Attack on Titan S4 - 16v2 (1080p) [B33FCAFE].mkv");

    assertEquals("Attack on Titan S4", result.showTitle());
    assertEquals(16, result.episodeNumber());
    assertEquals(2, result.version());
    assertEquals("b33fcafe", result.crc32());
  }

  @Test
  void recognizesTheLiteralWordBatchEvenWithoutAParenthesizedRange() {
    ParsedAnimeRelease result =
        parser.parse(
            "[Golumpa] Fullmetal Alchemist Brotherhood [BD 1080p Dual Audio HEVC10] [Batch]");

    assertTrue(result.batch());
    assertTrue(result.dualAudio());
  }

  @Test
  void doesNotCrashOnATitleWithNoRecognizableStructureAtAll() {
    ParsedAnimeRelease result = parser.parse("Some Random File.mkv");

    assertNull(result.releaseGroup());
    assertNull(result.episodeNumber());
    assertFalse(result.batch());
    assertEquals("Some Random File.mkv", result.showTitle());
  }

  @Test
  void preservesTheFullRawTitleUnmodified() {
    String raw = "[SubsPlease] Chainsaw Man - 01 (1080p) [12AB34CD].mkv";
    assertEquals(raw, parser.parse(raw).title());
  }
}
