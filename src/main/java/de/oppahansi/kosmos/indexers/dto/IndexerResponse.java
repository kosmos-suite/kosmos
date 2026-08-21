package de.oppahansi.kosmos.indexers.dto;

import de.oppahansi.kosmos.indexers.Indexer;
import java.time.Instant;
import java.util.UUID;

/** API representation of an {@link Indexer}. The API key itself is never returned. */
public record IndexerResponse(
    UUID id, String name, String baseUrl, boolean apiKeySet, boolean enabled, Instant createdAt) {

  public static IndexerResponse from(Indexer indexer) {
    return new IndexerResponse(
        indexer.id,
        indexer.name,
        indexer.baseUrl,
        indexer.apiKey != null && !indexer.apiKey.isBlank(),
        indexer.enabled,
        indexer.createdAt);
  }
}
