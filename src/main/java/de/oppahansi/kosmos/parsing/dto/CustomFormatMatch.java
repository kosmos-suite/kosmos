package de.oppahansi.kosmos.parsing.dto;

import java.util.UUID;

/**
 * One CustomFormat's contribution to a release's score — matched or not, feeding rejection-reason
 * UI.
 */
public record CustomFormatMatch(UUID customFormatId, String name, int score, boolean matched) {}
