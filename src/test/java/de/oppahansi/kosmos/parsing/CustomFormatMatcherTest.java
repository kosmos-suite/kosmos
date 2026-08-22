package de.oppahansi.kosmos.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

class CustomFormatMatcherTest {

  private final CustomFormatMatcher matcher = new CustomFormatMatcher();
  private final ReleaseParser parser = new ReleaseParser();

  CustomFormatMatcherTest() {
    matcher.objectMapper = new ObjectMapper();
  }

  private ParsedRelease parse(String title) {
    return parser.parse(title);
  }

  @Test
  void singleRequiredSpecMatches() {
    String rule =
        """
        [{"field":"resolution","matchType":"equals","value":"2160p","negate":false,"required":true}]
        """;
    assertTrue(
        matcher.matches(matcher.parseRule(rule), parse("The.Matrix.1999.2160p.WEB-DL.x265-GROUP")));
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void requiredSpecMustMatchAndAtLeastOneOptionalMustAlsoMatch() {
    // Mirrors TRaSH-Guides' "Remux Tier" shape: a mandatory condition plus a list of
    // alternative optional conditions, any one of which is enough.
    String rule =
        """
        [
          {"field":"source","matchType":"equals","value":"Blu-ray","negate":false,"required":true},
          {"field":"videoCodec","matchType":"equals","value":"H.264","negate":false,"required":false},
          {"field":"videoCodec","matchType":"equals","value":"H.265","negate":false,"required":false}
        ]
        """;
    assertTrue(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
    // required spec fails (Web, not Blu-ray) even though an optional one would match
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("The.Matrix.1999.2160p.WEB-DL.x265-GROUP")));
  }

  @Test
  void allOptionalSpecsFailingMeansNoMatchEvenIfRequiredPasses() {
    String rule =
        """
        [
          {"field":"source","matchType":"equals","value":"Blu-ray","negate":false,"required":true},
          {"field":"videoCodec","matchType":"equals","value":"H.265","negate":false,"required":false}
        ]
        """;
    // required (Blu-ray) matches, but the only optional spec (H.265) doesn't — release is x264
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void regexMatchTypeSearchesTitle() {
    String rule =
        """
        [{"field":"title","matchType":"regex","value":"matr.x","negate":false,"required":true}]
        """;
    assertTrue(
        matcher.matches(matcher.parseRule(rule), parse("The.Matrix.1999.2160p.WEB-DL.x265-GROUP")));
  }

  @Test
  void titleRegexMatchesTagsAfterTheYearNotJustTheMovieName() {
    // Regression test for a real bug found live: TRaSH-Guides' ReleaseTitleSpecification regexes
    // (imported by TrashGuidesImportService) target tags like REPACK/HDR that sit after the year in
    // a scene release name. If ParsedRelease.title() were trimmed to "the movie name before the
    // year" (its old behavior), every one of those imported formats would silently never match.
    String rule =
        """
        [{"field":"title","matchType":"regex","value":"\\\\bHDR\\\\b","negate":false,"required":true}]
        """;
    assertTrue(
        matcher.matches(
            matcher.parseRule(rule),
            parse("Inception.2010.2160p.UHD.BluRay.REPACK.HDR.x265-GROUP")));
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void negateFlipsResult() {
    String rule =
        """
        [{"field":"proper","matchType":"equals","value":"true","negate":true,"required":true}]
        """;
    // not proper -> negated equals-true check passes
    assertTrue(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void missingParsedFieldNeverMatchesRegardlessOfNegate() {
    String matches =
        """
        [{"field":"edition","matchType":"equals","value":"Director's Cut","negate":false,"required":true}]
        """;
    assertFalse(
        matcher.matches(
            matcher.parseRule(matches), parse("Inception.2010.1080p.BluRay.x264-GROUP")));

    String negated =
        """
        [{"field":"edition","matchType":"equals","value":"Director's Cut","negate":true,"required":true}]
        """;
    assertTrue(
        matcher.matches(
            matcher.parseRule(negated), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void releaseGroupFieldMatchesTrailingHyphenToken() {
    // Mirrors TRaSH-Guides' ReleaseGroupSpecification shape (see the trash import service).
    String rule =
        """
        [{"field":"releaseGroup","matchType":"regex","value":"^(GROUP)$","negate":false,"required":true}]
        """;
    assertTrue(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-OTHER")));
  }

  @Test
  void unknownFieldRejectedAtParseTime() {
    String rule =
        """
        [{"field":"nonsense","matchType":"equals","value":"x","negate":false,"required":true}]
        """;
    assertThrows(BadRequestException.class, () -> matcher.parseRule(rule));
  }

  @Test
  void unknownMatchTypeRejectedAtParseTime() {
    String rule =
        """
        [{"field":"title","matchType":"fuzzy","value":"x","negate":false,"required":true}]
        """;
    assertThrows(BadRequestException.class, () -> matcher.parseRule(rule));
  }

  @Test
  void malformedJsonRejected() {
    assertThrows(BadRequestException.class, () -> matcher.parseRule("not json"));
    assertThrows(BadRequestException.class, () -> matcher.parseRule("{\"not\":\"an array\"}"));
  }

  @Test
  void emptySpecListMatchesVacuously() {
    assertEquals(0, matcher.parseRule("[]").size());
    assertTrue(
        matcher.matches(matcher.parseRule("[]"), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void remuxConditionMatchesOnlyRemuxReleases() {
    String rule =
        """
        [{"field":"remux","matchType":"equals","value":"true","negate":false,"required":true}]
        """;
    assertTrue(
        matcher.matches(
            matcher.parseRule(rule), parse("A.Movie.2021.2160p.BluRay.REMUX.x265-GROUP")));
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("A.Movie.2021.1080p.BluRay.x264-GROUP")));
  }

  @Test
  void seasonPackConditionMatchesAPackButNotASingleEpisode() {
    String rule =
        """
        [{"field":"seasonPack","matchType":"equals","value":"true","negate":false,"required":true}]
        """;
    assertTrue(
        matcher.matches(matcher.parseRule(rule), parse("Show.Name.S01.1080p.WEB-DL.x264-GROUP")));
    assertFalse(
        matcher.matches(
            matcher.parseRule(rule), parse("Show.Name.S01E05.1080p.WEB-DL.x264-GROUP")));
    assertFalse(
        matcher.matches(matcher.parseRule(rule), parse("Inception.2010.1080p.BluRay.x264-GROUP")));
  }
}
