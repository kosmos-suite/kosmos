package de.oppahansi.kosmos.media.dto;

import de.oppahansi.kosmos.media.AnimeEpisode;
import de.oppahansi.kosmos.media.AnimeSeason;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AnimeSeasonResponse(
    UUID id,
    int seasonNumber,
    String name,
    String overview,
    Integer episodeCount,
    List<AnimeEpisodeResponse> episodes) {

  public static AnimeSeasonResponse from(
      AnimeSeason season, List<AnimeEpisode> episodes, Map<UUID, String> statusByEpisode) {
    return new AnimeSeasonResponse(
        season.id,
        season.seasonNumber,
        season.name,
        season.overview,
        season.episodeCount,
        episodes.stream()
            .map(
                e ->
                    AnimeEpisodeResponse.from(
                        e, statusByEpisode.getOrDefault(e.mediaItemId, "MISSING")))
            .toList());
  }
}
