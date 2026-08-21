package de.oppahansi.kosmos.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import org.junit.jupiter.api.Test;

class ReleaseParserTest {

  private final ReleaseParser parser = new ReleaseParser();

  @Test
  void parsesStandardBluRayRelease() {
    ParsedRelease result = parser.parse("Inception.2010.1080p.BluRay.x264-GROUP");

    assertEquals(2010, result.year());
    assertEquals("1080p", result.resolution());
    assertEquals("Blu-ray", result.source());
    assertEquals("H.264", result.videoCodec());
    assertFalse(result.proper());
    assertFalse(result.repack());
  }

  @Test
  void parsesWebDlWithHyphenVariant() {
    ParsedRelease result = parser.parse("The.Matrix.1999.2160p.WEB-DL.x265-GROUP");

    assertEquals(1999, result.year());
    assertEquals("2160p", result.resolution());
    assertEquals("Web", result.source());
    assertEquals("H.265", result.videoCodec());
  }

  @Test
  void parsesSpaceSeparatedTitleWithProperBeforeYear() {
    ParsedRelease result = parser.parse("Some Movie PROPER 2019 1080p BluRay x264-GROUP");

    assertTrue(result.proper());
    assertFalse(result.repack());
  }

  @Test
  void titleFieldPreservesTheFullRawReleaseStringUnmodified() {
    // RuleSpecification's "title" field (see CustomFormatMatcher) matches against this verbatim —
    // TRaSH-Guides' ReleaseTitleSpecification regexes are written against the whole scene release
    // name, not just the movie name before the year, so trimming it here would silently break
    // every title-regex custom format for anything tagged after the year (REPACK, HDR, x265, …).
    String raw = "Inception.2010.2160p.UHD.BluRay.REPACK.HDR.x265-GROUP";

    assertEquals(raw, parser.parse(raw).title());
  }

  @Test
  void parsesRepackFlag() {
    ParsedRelease result = parser.parse("Another.Movie.2020.REPACK.1080p.WEBRip.x264-GROUP");

    assertTrue(result.repack());
    assertFalse(result.proper());
    assertEquals("Web", result.source());
  }

  @Test
  void parsesRealProperAsProperNotRepack() {
    ParsedRelease result = parser.parse("A.Movie.2021.REAL.PROPER.1080p.BluRay.x264-GROUP");

    assertTrue(result.proper());
    assertFalse(result.repack());
  }

  @Test
  void parsesAudioCodecAndDoesNotConfuseDtsHdWithDts() {
    ParsedRelease result = parser.parse("A.Movie.2018.2160p.UHD.BluRay.DTS-HD.MA.5.1.x265-GROUP");

    assertEquals("DTS-HD", result.audioCodec());
  }

  @Test
  void parsesPlainDtsWhenNoHdSuffixPresent() {
    ParsedRelease result = parser.parse("A.Movie.2018.1080p.BluRay.DTS.x264-GROUP");

    assertEquals("DTS", result.audioCodec());
  }

  @Test
  void parsesEditionAndHdtvSource() {
    ParsedRelease result = parser.parse("A.Movie.2015.EXTENDED.720p.HDTV.XviD-GROUP");

    assertEquals("Extended", result.edition());
    assertEquals("HDTV", result.source());
    assertEquals("Xvid", result.videoCodec());
  }

  @Test
  void leavesFieldsNullWhenNotPresent() {
    ParsedRelease result = parser.parse("A Title With No Structured Info");

    assertNull(result.year());
    assertNull(result.resolution());
    assertNull(result.source());
    assertNull(result.videoCodec());
    assertNull(result.audioCodec());
    assertNull(result.edition());
    assertNull(result.releaseGroup());
    assertNull(result.seasonNumber());
    assertNull(result.episodeNumber());
    assertEquals("A Title With No Structured Info", result.title());
  }

  @Test
  void parsesStandardSeasonEpisodeMarker() {
    ParsedRelease result = parser.parse("Show.Name.S01E05.1080p.WEB-DL.x264-GROUP");

    assertEquals(1, result.seasonNumber());
    assertEquals(5, result.episodeNumber());
  }

  @Test
  void parsesFirstEpisodeOfAMultiEpisodeRelease() {
    ParsedRelease result = parser.parse("Show.Name.S01E05E06.1080p.WEB-DL.x264-GROUP");

    assertEquals(1, result.seasonNumber());
    assertEquals(5, result.episodeNumber());
  }

  @Test
  void parsesNxNnShorthandSeasonEpisode() {
    ParsedRelease result = parser.parse("Show.Name.10x05.720p.HDTV.x264-GROUP");

    assertEquals(10, result.seasonNumber());
    assertEquals(5, result.episodeNumber());
  }

  @Test
  void parsesSeasonPackWithNoEpisodeMarker() {
    ParsedRelease result = parser.parse("Show.Name.S02.1080p.WEB-DL.x264-GROUP");

    assertEquals(2, result.seasonNumber());
    assertNull(result.episodeNumber());
  }

  @Test
  void doesNotMistakeAResolutionLikeTokenForSeasonEpisode() {
    ParsedRelease result = parser.parse("A.Movie.2020.1080p.BluRay.x264-GROUP");

    assertNull(result.seasonNumber());
    assertNull(result.episodeNumber());
  }

  @Test
  void extractsReleaseGroupFromTrailingHyphenToken() {
    ParsedRelease result = parser.parse("Inception.2010.1080p.BluRay.x264-GROUP");

    assertEquals("GROUP", result.releaseGroup());
  }

  @Test
  void releaseGroupIsNullWhenNoTrailingHyphenToken() {
    ParsedRelease result = parser.parse("Inception 2010 1080p BluRay x264");

    assertNull(result.releaseGroup());
  }
}
