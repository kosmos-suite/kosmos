package de.oppahansi.kosmos.media.dto;

import java.util.List;
import java.util.UUID;

/**
 * A TMDB collection's full detail — its members plus (if monitored) the local {@link
 * MovieCollectionResponse} row.
 */
public record MovieCollectionDetailResponse(
    String tmdbCollectionId,
    String name,
    String posterPath,
    String backdropPath,
    boolean monitored,
    UUID movieCollectionId,
    List<MovieCollectionMemberResponse> members) {}
