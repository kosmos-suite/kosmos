package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** The subset of TMDB's {@code /movie/{id}} response Kosmos actually uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieDetails(Integer runtime, @JsonProperty("release_date") String releaseDate) {

  /** {@link #releaseDate} parsed, or {@code null} if absent/not a valid ISO date. */
  public LocalDate releaseDateAsLocalDate() {
    if (releaseDate == null || releaseDate.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(releaseDate);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
