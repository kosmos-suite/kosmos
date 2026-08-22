package de.oppahansi.kosmos.jellyfin.dto;

import de.oppahansi.kosmos.jellyfin.JellyfinServer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record JellyfinServerResponse(
    UUID id,
    String name,
    String baseUrl,
    boolean apiKeySet,
    boolean enabled,
    Instant createdAt,
    List<String> selectedLibraryIds,
    List<String> selectedUserIds) {

  public static JellyfinServerResponse from(JellyfinServer server) {
    return new JellyfinServerResponse(
        server.id,
        server.name,
        server.baseUrl,
        server.apiKey != null && !server.apiKey.isBlank(),
        server.enabled,
        server.createdAt,
        split(server.selectedLibraryIds),
        split(server.selectedUserIds));
  }

  private static List<String> split(String commaSeparated) {
    return commaSeparated == null || commaSeparated.isBlank()
        ? List.of()
        : Arrays.asList(commaSeparated.split(","));
  }
}
