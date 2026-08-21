package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The subset of TMDB's {@code /tv/{id}/season/{n}} response Kosmos uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbSeasonDetails(List<Episode> episodes) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Episode(
      @JsonProperty("episode_number") int episodeNumber,
      String name,
      String overview,
      @JsonProperty("air_date") String airDate,
      Integer runtime,
      @JsonProperty("still_path") String stillPath) {}
}
