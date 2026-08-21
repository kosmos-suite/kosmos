package de.oppahansi.kosmos.downloads.dto;

import de.oppahansi.kosmos.downloads.DownloadClient;
import java.time.Instant;
import java.util.UUID;

/** API representation of a {@link DownloadClient}. The password itself is never returned. */
public record DownloadClientResponse(
    UUID id,
    String name,
    String type,
    String baseUrl,
    String username,
    boolean passwordSet,
    String category,
    boolean enabled,
    Instant createdAt) {

  public static DownloadClientResponse from(DownloadClient client) {
    return new DownloadClientResponse(
        client.id,
        client.name,
        client.type,
        client.baseUrl,
        client.username,
        client.password != null && !client.password.isBlank(),
        client.category,
        client.enabled,
        client.createdAt);
  }
}
