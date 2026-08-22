package de.oppahansi.kosmos.parsing.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Create/update payload for a {@link de.oppahansi.kosmos.parsing.QualityProfile}. {@code
 * grabDelayMinutes} defaults to 0 (grab immediately, existing behavior) and {@code bypassScore} is
 * optional — see the entity's own doc.
 */
public record QualityProfileRequest(
    String name,
    int cutoffScore,
    Set<UUID> customFormatIds,
    int grabDelayMinutes,
    Integer bypassScore) {}
