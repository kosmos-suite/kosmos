package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The {@code credits} part of a movie/tv detail response's {@code append_to_response}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbCredits(List<Cast> cast, List<Crew> crew) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Cast(String name, String character, @JsonProperty("profile_path") String profilePath) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Crew(String name, String job) {}
}
