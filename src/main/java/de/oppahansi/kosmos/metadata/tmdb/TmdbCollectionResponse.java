package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** TMDB's raw {@code /collection/{id}} response. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbCollectionResponse(
    int id,
    String name,
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("backdrop_path") String backdropPath,
    List<TmdbMovie> parts) {}
