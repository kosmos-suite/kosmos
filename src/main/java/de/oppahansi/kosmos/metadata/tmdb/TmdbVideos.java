package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** The {@code videos} part of an {@code append_to_response} movie/TV detail call. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbVideos(List<Video> results) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Video(String key, String site, String type, Boolean official) {}
}
