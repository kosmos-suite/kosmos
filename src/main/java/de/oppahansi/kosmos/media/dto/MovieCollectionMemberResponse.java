package de.oppahansi.kosmos.media.dto;

import java.util.UUID;

public record MovieCollectionMemberResponse(
    String externalId,
    String title,
    Integer year,
    String posterPath,
    boolean inLibrary,
    UUID mediaItemId) {}
