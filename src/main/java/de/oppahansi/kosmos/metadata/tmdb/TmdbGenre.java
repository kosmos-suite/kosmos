package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenre(int id, String name) {}
