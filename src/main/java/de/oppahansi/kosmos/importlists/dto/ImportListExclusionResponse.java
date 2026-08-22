package de.oppahansi.kosmos.importlists.dto;

import de.oppahansi.kosmos.importlists.ImportListExclusion;
import java.time.Instant;
import java.util.UUID;

public record ImportListExclusionResponse(
    UUID id, String pluginSlug, String externalId, String title, Instant excludedAt) {

  public static ImportListExclusionResponse from(ImportListExclusion exclusion) {
    return new ImportListExclusionResponse(
        exclusion.id,
        exclusion.pluginSlug,
        exclusion.externalId,
        exclusion.title,
        exclusion.excludedAt);
  }
}
