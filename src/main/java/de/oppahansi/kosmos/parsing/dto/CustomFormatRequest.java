package de.oppahansi.kosmos.parsing.dto;

/** Create/update payload for a {@link de.oppahansi.kosmos.parsing.CustomFormat}. */
public record CustomFormatRequest(String name, int score, String rule) {}
