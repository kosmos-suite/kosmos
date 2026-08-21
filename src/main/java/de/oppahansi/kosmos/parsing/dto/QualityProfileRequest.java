package de.oppahansi.kosmos.parsing.dto;

import java.util.Set;
import java.util.UUID;

/** Create/update payload for a {@link de.oppahansi.kosmos.parsing.QualityProfile}. */
public record QualityProfileRequest(String name, int cutoffScore, Set<UUID> customFormatIds) {}
