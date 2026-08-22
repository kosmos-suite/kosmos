package de.oppahansi.kosmos.importlists.dto;

import de.oppahansi.kosmos.importlists.ImportList;
import java.time.Instant;
import java.util.UUID;

public record ImportListResponse(
    UUID id,
    String name,
    String sourceType,
    String mediaType,
    boolean enabled,
    boolean trusted,
    UUID qualityProfileId,
    String qualityProfileName,
    Instant lastSyncedAt) {

  public static ImportListResponse from(ImportList list) {
    return new ImportListResponse(
        list.id,
        list.name,
        list.sourceType,
        list.parsedSourceType().mediaType,
        list.enabled,
        list.trusted,
        list.qualityProfile != null ? list.qualityProfile.id : null,
        list.qualityProfile != null ? list.qualityProfile.name : null,
        list.lastSyncedAt);
  }
}
