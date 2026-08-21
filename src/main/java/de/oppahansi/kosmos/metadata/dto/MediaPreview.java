package de.oppahansi.kosmos.metadata.dto;

import java.util.List;

/**
 * A not-yet-owned title's detail page — same shape as {@link MediaDetailExtras} plus the base
 * TMDB/AniList fields (title, poster, overview, ...) a real {@code Movie}/{@code Show}/{@code
 * Anime} row would otherwise carry, since none exists yet. Backs {@code GET
 * /movies|shows/tmdb/{externalId}} and {@code GET /anime/anilist/{externalId}} — the "Add to
 * Library"/"Request" screen a card for a title Kosmos doesn't have yet links to, instead of falling
 * back to a search.
 */
public record MediaPreview(
    String externalId,
    String pluginSlug,
    String mediaType,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    List<String> genres,
    List<MediaDetailExtras.Fact> facts,
    Double voteAverage,
    Integer voteCount,
    String certification,
    List<MediaDetailExtras.CastMember> cast,
    List<MetadataSearchItem> similar) {}
