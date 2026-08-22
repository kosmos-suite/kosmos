package de.oppahansi.kosmos.downloads.dto;

import de.oppahansi.kosmos.downloads.Blocklist;
import java.time.Instant;
import java.util.UUID;

public record BlocklistResponse(
    UUID id,
    UUID mediaItemId,
    String mediaItemTitle,
    String titleRaw,
    String reason,
    Instant blockedAt) {

  public static BlocklistResponse from(Blocklist entry) {
    return new BlocklistResponse(
        entry.id,
        entry.mediaItem.id,
        entry.mediaItem.title,
        entry.titleRaw,
        entry.reason,
        entry.blockedAt);
  }
}
