package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbGenreListResponse(List<TmdbGenre> genres) {}
