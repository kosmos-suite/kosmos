package de.oppahansi.kosmos.parsing.dto;

import java.util.List;

public record ScoredRelease(
    ParsedRelease release,
    int totalScore,
    int cutoffScore,
    boolean passesCutoff,
    List<CustomFormatMatch> formatBreakdown) {}
