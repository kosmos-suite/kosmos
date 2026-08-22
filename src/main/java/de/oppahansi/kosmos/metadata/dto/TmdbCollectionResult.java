package de.oppahansi.kosmos.metadata.dto;

import java.util.List;

/**
 * TMDB's {@code /collection/{id}} response, mapped down to what {@code MovieCollectionService}
 * needs.
 */
public record TmdbCollectionResult(
    String externalId,
    String name,
    String posterPath,
    String backdropPath,
    List<MetadataSearchResult> members) {}
