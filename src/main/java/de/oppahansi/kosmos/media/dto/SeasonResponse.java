package de.oppahansi.kosmos.media.dto;

import de.oppahansi.kosmos.media.Episode;
import de.oppahansi.kosmos.media.Season;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SeasonResponse(
    UUID id,
    int seasonNumber,
    String name,
    String overview,
    String posterPath,
    Integer episodeCount,
    List<EpisodeResponse> episodes) {

  public static SeasonResponse from(
      Season season, List<Episode> episodes, Map<UUID, String> statusByEpisode) {
    return new SeasonResponse(
        season.id,
        season.seasonNumber,
        season.name,
        season.overview,
        season.posterPath,
        season.episodeCount,
        episodes.stream()
            .map(
                e ->
                    EpisodeResponse.from(e, statusByEpisode.getOrDefault(e.mediaItemId, "MISSING")))
            .toList());
  }
}
