package de.oppahansi.kosmos.media.dto;

import java.util.UUID;

/**
 * Payload for adding a movie, typically from a {@link
 * de.oppahansi.kosmos.metadata.MetadataProvider} search result. {@code qualityProfileId} is
 * optional — leaving it null adds the movie unmonitored (see {@link
 * de.oppahansi.kosmos.media.Movie#qualityProfile}).
 */
public record CreateMovieRequest(
    String externalId,
    String pluginSlug,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    UUID qualityProfileId,
    UUID rootFolderId) {}
