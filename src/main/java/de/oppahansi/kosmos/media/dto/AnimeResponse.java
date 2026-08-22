package de.oppahansi.kosmos.media.dto;

import de.oppahansi.kosmos.media.Anime;
import java.time.Instant;
import java.util.UUID;

/** Summary shape for listing anime — no episode list, see {@link AnimeDetailResponse}. */
public record AnimeResponse(
    UUID id,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    String status,
    Integer episodeCountTotal,
    Instant addedAt,
    UUID qualityProfileId,
    boolean partiallyAvailable) {

  public static AnimeResponse from(Anime anime, boolean partiallyAvailable) {
    return new AnimeResponse(
        anime.mediaItemId,
        anime.mediaItem.title,
        anime.mediaItem.year,
        anime.overview,
        anime.posterPath,
        anime.backdropPath,
        anime.status,
        anime.episodeCountTotal,
        anime.mediaItem.addedAt,
        anime.qualityProfile == null ? null : anime.qualityProfile.id,
        partiallyAvailable);
  }
}
