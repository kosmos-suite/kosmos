package de.oppahansi.kosmos.indexers.dto;

import java.time.Instant;

/** A single search result from a Torznab-compatible indexer. */
public record TorznabResult(
    String title,
    String downloadUrl,
    long sizeBytes,
    Integer seeders,
    Integer peers,
    Instant publishedAt) {}
