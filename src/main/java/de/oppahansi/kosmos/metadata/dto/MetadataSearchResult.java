package de.oppahansi.kosmos.metadata.dto;

/**
 * A single search result from a {@link de.oppahansi.kosmos.metadata.MetadataProvider}. {@code
 * mediaType} is "movie" or "tv" — which /movies or /shows endpoint to POST to next. {@code
 * backdropPath} rides along so a movie/show/anime created from this result can store it for its own
 * detail-page hero — see {@code media.Movie#backdropPath} and friends. {@code voteAverage} is only
 * ever populated for movie results today (TMDB's 0-10 scale) — it exists here rather than only on
 * {@code DiscoverItem} because {@code DiscoverService} builds trending/popular rows directly from
 * this record.
 */
public record MetadataSearchResult(
    String externalId,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    Double voteAverage,
    String mediaType) {}
