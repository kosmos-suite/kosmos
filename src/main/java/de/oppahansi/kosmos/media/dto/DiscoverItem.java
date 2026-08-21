package de.oppahansi.kosmos.media.dto;

import java.util.UUID;

/**
 * One row in a Discover/Home browsing list — either a real library item (movie or show already
 * added, {@code mediaItemId} set) or a TMDB result not yet added ({@code externalId} set, {@code
 * mediaItemId} null). Deliberately the same shape for both "recently added" (100% Kosmos's own
 * data) and "trending"/"popular" (TMDB, cross-referenced against the library) rows, since the
 * frontend renders every Discover row with the same card. {@code backdropPath}/{@code voteAverage}
 * back the Home hero specifically (see {@code HomePage.tsx}'s {@code Hero} component) — {@code
 * voteAverage} is only ever populated for trending/popular (TMDB movies), null for "recently added"
 * rows since those are pulled from the library, not re-fetched from TMDB.
 */
public record DiscoverItem(
    UUID mediaItemId,
    String externalId,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    Double voteAverage,
    String mediaType,
    boolean inLibrary,
    boolean partiallyAvailable) {}
