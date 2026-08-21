package de.oppahansi.kosmos.jellyfin.dto;

import de.oppahansi.kosmos.jellyfin.JellyfinServer;
import java.time.Instant;
import java.util.UUID;

public record JellyfinServerResponse(
    UUID id, String name, String baseUrl, boolean apiKeySet, boolean enabled, Instant createdAt) {

  public static JellyfinServerResponse from(JellyfinServer server) {
    return new JellyfinServerResponse(
        server.id,
        server.name,
        server.baseUrl,
        server.apiKey != null && !server.apiKey.isBlank(),
        server.enabled,
        server.createdAt);
  }
}
