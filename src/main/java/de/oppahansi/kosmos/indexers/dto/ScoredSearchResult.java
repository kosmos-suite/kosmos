package de.oppahansi.kosmos.indexers.dto;

import de.oppahansi.kosmos.parsing.dto.CustomFormatMatch;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import java.util.List;

/**
 * A raw Torznab result plus its parsed release attributes. Score-related fields are null unless the
 * search request supplied a qualityProfileId to score against.
 */
public record ScoredSearchResult(
    TorznabResult raw,
    ParsedRelease parsed,
    Integer score,
    Integer cutoffScore,
    Boolean passesCutoff,
    List<CustomFormatMatch> formatBreakdown,
    /** Non-null means the hard size gate rejected this release — see QualityDefinitionService. */
    String sizeGateReason,
    /**
     * Non-null means this exact release already failed for the media item the search was scoped to
     * (see {@code IndexerResource}'s {@code mediaItemId} query param) — the reason a prior {@code
     * Blocklist} entry recorded. Still returned, not filtered out, so the UI can show why and still
     * offer an override-grab, the same as every other rejection reason here.
     */
    String blocklistReason) {

  public static ScoredSearchResult unscored(TorznabResult raw, ParsedRelease parsed) {
    return new ScoredSearchResult(raw, parsed, null, null, null, null, null, null);
  }
}
