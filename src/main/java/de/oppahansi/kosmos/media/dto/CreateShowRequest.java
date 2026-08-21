package de.oppahansi.kosmos.media.dto;

import java.util.UUID;

/**
 * Payload for adding a show, typically from a {@link de.oppahansi.kosmos.metadata.MetadataProvider}
 * TV search result. Fetches the full season/episode tree from TMDB as part of creation — see {@code
 * media.ShowService}.
 */
public record CreateShowRequest(
    String externalId,
    String pluginSlug,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    UUID qualityProfileId,
    UUID rootFolderId) {}
