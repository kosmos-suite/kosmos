package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.CustomFormatMatch;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import de.oppahansi.kosmos.parsing.dto.ScoredRelease;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** Evaluates a {@link ParsedRelease} against a {@link QualityProfile}'s custom formats. */
@ApplicationScoped
public class ScoringEngine {

  @Inject CustomFormatMatcher matcher;

  public ScoredRelease score(ParsedRelease release, QualityProfile profile) {
    List<CustomFormatMatch> breakdown =
        profile.customFormats.stream()
            .map(
                format -> {
                  boolean matched = matcher.matches(format, release);
                  return new CustomFormatMatch(
                      format.id, format.name, matched ? format.score : 0, matched);
                })
            .toList();

    int totalScore = breakdown.stream().mapToInt(CustomFormatMatch::score).sum();
    return new ScoredRelease(
        release, totalScore, profile.cutoffScore, totalScore >= profile.cutoffScore, breakdown);
  }
}
