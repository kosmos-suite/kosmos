package de.oppahansi.kosmos.media.dto;

import de.oppahansi.kosmos.media.Anime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnimeDetailResponse(
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
    List<AnimeEpisodeResponse> episodes) {

  public static AnimeDetailResponse from(Anime anime, List<AnimeEpisodeResponse> episodes) {
    return new AnimeDetailResponse(
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
        episodes);
  }
}
