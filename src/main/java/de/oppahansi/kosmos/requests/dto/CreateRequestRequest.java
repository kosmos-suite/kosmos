package de.oppahansi.kosmos.requests.dto;

import java.util.UUID;

public record CreateRequestRequest(
    String externalId,
    String pluginSlug,
    String mediaType,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    UUID qualityProfileId) {}
