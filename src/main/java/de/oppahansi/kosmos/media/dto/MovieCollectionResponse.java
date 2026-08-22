package de.oppahansi.kosmos.media.dto;

import de.oppahansi.kosmos.media.MovieCollection;
import java.time.Instant;
import java.util.UUID;

public record MovieCollectionResponse(
    UUID id,
    String tmdbCollectionId,
    String name,
    String posterPath,
    String backdropPath,
    boolean monitored,
    UUID qualityProfileId,
    String qualityProfileName,
    Instant lastSyncedAt) {

  public static MovieCollectionResponse from(MovieCollection collection) {
    return new MovieCollectionResponse(
        collection.id,
        collection.tmdbCollectionId,
        collection.name,
        collection.posterPath,
        collection.backdropPath,
        collection.monitored,
        collection.qualityProfile != null ? collection.qualityProfile.id : null,
        collection.qualityProfile != null ? collection.qualityProfile.name : null,
        collection.lastSyncedAt);
  }
}
