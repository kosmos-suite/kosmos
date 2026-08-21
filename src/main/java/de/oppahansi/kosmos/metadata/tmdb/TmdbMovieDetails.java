package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The subset of TMDB's {@code /movie/{id}} response Kosmos actually uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbMovieDetails(Integer runtime) {}
