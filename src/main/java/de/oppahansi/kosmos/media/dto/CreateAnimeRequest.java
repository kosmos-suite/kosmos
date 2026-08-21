package de.oppahansi.kosmos.media.dto;

import java.util.UUID;

/**
 * Payload for adding an anime, typically from a {@link
 * de.oppahansi.kosmos.metadata.anilist.AniListMetadataProvider} search result. Fetches AniList's
 * episode count as part of creation and generates a flat run of {@code AnimeEpisode} placeholders
 * from it — see {@code media.AnimeService}.
 */
public record CreateAnimeRequest(
    String externalId,
    String pluginSlug,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    UUID qualityProfileId) {}
