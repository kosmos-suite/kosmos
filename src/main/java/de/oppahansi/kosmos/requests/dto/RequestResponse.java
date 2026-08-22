package de.oppahansi.kosmos.requests.dto;

import de.oppahansi.kosmos.library.LibraryFile;
import de.oppahansi.kosmos.requests.Request;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code status} is APPROVED/AVAILABLE split at read time, not storage: a {@link Request} only ever
 * persists PENDING/APPROVED/DECLINED, and "available" is computed here from whether a {@link
 * LibraryFile} now exists for the linked media item — no separate job needed to keep it in sync.
 */
public record RequestResponse(
    UUID id,
    String requestedByDisplayName,
    boolean mine,
    String mediaType,
    String externalId,
    String pluginSlug,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    UUID qualityProfileId,
    String qualityProfileName,
    String status,
    String note,
    UUID mediaItemId,
    Instant requestedAt,
    Instant decidedAt) {

  public static RequestResponse from(Request request, UUID currentUserId) {
    boolean available =
        "APPROVED".equals(request.status)
            && request.mediaItem != null
            && LibraryFile.count("mediaItem.id", request.mediaItem.id) > 0;
    return new RequestResponse(
        request.id,
        request.sourceListName != null ? request.sourceListName : request.requestedBy.displayName,
        request.requestedBy != null && request.requestedBy.id.equals(currentUserId),
        request.mediaType,
        request.externalId,
        request.pluginSlug,
        request.title,
        request.year,
        request.overview,
        request.posterPath,
        request.backdropPath,
        request.qualityProfile != null ? request.qualityProfile.id : null,
        request.qualityProfile != null ? request.qualityProfile.name : null,
        available ? "AVAILABLE" : request.status,
        request.note,
        request.mediaItem != null ? request.mediaItem.id : null,
        request.requestedAt,
        request.decidedAt);
  }
}
