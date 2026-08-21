package de.oppahansi.kosmos.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.dto.CustomFormatMatch;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import de.oppahansi.kosmos.parsing.dto.ScoredRelease;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScoringEngineTest {

  private final ScoringEngine engine = new ScoringEngine();
  private final ReleaseParser parser = new ReleaseParser();

  ScoringEngineTest() {
    engine.matcher = new CustomFormatMatcher();
    engine.matcher.objectMapper = new ObjectMapper();
  }

  private CustomFormat customFormat(String name, int score, String rule) {
    CustomFormat format = new CustomFormat();
    format.name = name;
    format.score = score;
    format.rule = rule;
    return format;
  }

  @Test
  void sumsScoreOfOnlyMatchingFormats() {
    CustomFormat remux =
        customFormat(
            "Remux",
            50,
            """
            [{"field":"source","matchType":"equals","value":"Blu-ray","negate":false,"required":true}]
            """);
    CustomFormat hevc =
        customFormat(
            "HEVC",
            10,
            """
            [{"field":"videoCodec","matchType":"equals","value":"H.265","negate":false,"required":true}]
            """);

    QualityProfile profile = new QualityProfile();
    profile.name = "HD";
    profile.cutoffScore = 40;
    profile.customFormats = Set.of(remux, hevc);

    ParsedRelease release = parser.parse("Inception.2010.1080p.BluRay.x264-GROUP");
    ScoredRelease scored = engine.score(release, profile);

    assertEquals(50, scored.totalScore()); // Remux matches (+50), HEVC doesn't (x264, not x265)
    assertTrue(scored.passesCutoff());
    assertEquals(2, scored.formatBreakdown().size());

    CustomFormatMatch remuxMatch =
        scored.formatBreakdown().stream()
            .filter(m -> m.name().equals("Remux"))
            .findFirst()
            .orElseThrow();
    assertTrue(remuxMatch.matched());
    assertEquals(50, remuxMatch.score());

    CustomFormatMatch hevcMatch =
        scored.formatBreakdown().stream()
            .filter(m -> m.name().equals("HEVC"))
            .findFirst()
            .orElseThrow();
    assertFalse(hevcMatch.matched());
    assertEquals(0, hevcMatch.score());
  }

  @Test
  void belowCutoffScoreFailsEvenWithMatches() {
    CustomFormat weak =
        customFormat(
            "Weak",
            5,
            """
            [{"field":"source","matchType":"equals","value":"Blu-ray","negate":false,"required":true}]
            """);

    QualityProfile profile = new QualityProfile();
    profile.name = "Strict";
    profile.cutoffScore = 100;
    profile.customFormats = Set.of(weak);

    ScoredRelease scored =
        engine.score(parser.parse("Inception.2010.1080p.BluRay.x264-GROUP"), profile);

    assertEquals(5, scored.totalScore());
    assertFalse(scored.passesCutoff());
  }

  @Test
  void noCustomFormatsMeansZeroScore() {
    QualityProfile profile = new QualityProfile();
    profile.name = "Empty";
    profile.cutoffScore = 0;
    profile.customFormats = Set.of();

    ScoredRelease scored =
        engine.score(parser.parse("Inception.2010.1080p.BluRay.x264-GROUP"), profile);

    assertEquals(0, scored.totalScore());
    assertTrue(scored.passesCutoff()); // 0 >= cutoff of 0
    assertEquals(List.of(), scored.formatBreakdown());
  }
}
