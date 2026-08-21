package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The subset of TMDB's {@code /tv/{id}} response Kosmos uses — season summaries, not episodes. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbTvDetails(String status, List<Season> seasons) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Season(
      @JsonProperty("season_number") int seasonNumber,
      String name,
      String overview,
      @JsonProperty("poster_path") String posterPath,
      @JsonProperty("episode_count") Integer episodeCount) {}
}
