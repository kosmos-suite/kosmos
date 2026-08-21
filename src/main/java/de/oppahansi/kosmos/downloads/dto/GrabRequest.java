package de.oppahansi.kosmos.downloads.dto;

import java.util.UUID;

/**
 * Payload to send a chosen release to a download client. Mirrors {@link
 * de.oppahansi.kosmos.indexers.Release}'s parsed fields.
 */
public record GrabRequest(
    String title,
    String downloadUrl,
    String resolution,
    String source,
    String videoCodec,
    Integer score,
    UUID downloadClientId) {}
