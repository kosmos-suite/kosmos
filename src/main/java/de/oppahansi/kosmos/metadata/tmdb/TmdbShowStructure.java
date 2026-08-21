package de.oppahansi.kosmos.metadata.tmdb;

import java.time.LocalDate;
import java.util.List;

/**
 * A show's full season/episode tree, fetched from TMDB at show-creation time by {@link
 * TmdbMetadataProvider#fetchShowStructure}. Kosmos-native shape (not a raw TMDB DTO) so {@code
 * media.ShowService} doesn't need to know anything about TMDB's response format.
 */
public record TmdbShowStructure(String status, List<SeasonData> seasons) {

  public record SeasonData(
      int seasonNumber,
      String name,
      String overview,
      String posterPath,
      Integer episodeCount,
      List<EpisodeData> episodes) {}

  public record EpisodeData(
      int episodeNumber,
      String title,
      String overview,
      LocalDate airDate,
      Integer runtimeMinutes,
      String stillPath) {}
}
