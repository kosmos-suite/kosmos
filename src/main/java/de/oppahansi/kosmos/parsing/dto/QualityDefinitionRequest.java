package de.oppahansi.kosmos.parsing.dto;

/** Create/update payload for a {@link de.oppahansi.kosmos.parsing.QualityDefinition}. */
public record QualityDefinitionRequest(
    String resolution, String source, double minMbPerMinute, double maxMbPerMinute) {}
