package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/**
 * The subset of TMDB's {@code /movie/{id}?append_to_response=release_dates} response Kosmos
 * actually uses. {@code release_dates} backs {@link #digitalReleaseDateUs()} — Minimum
 * Availability's "Released" milestone gates automatic search on this rather than the theatrical
 * date, the same reasoning Radarr uses to avoid grabbing cam-quality day-one releases.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieDetails(
    Integer runtime,
    @JsonProperty("release_date") String releaseDate,
    @JsonProperty("release_dates") ReleaseDates releaseDates) {

  /** {@link #releaseDate} parsed, or {@code null} if absent/not a valid ISO date. */
  public LocalDate releaseDateAsLocalDate() {
    return parse(releaseDate);
  }

  /** Earliest US digital release (TMDB type 4), or {@code null} if TMDB has none on file yet. */
  public LocalDate digitalReleaseDateUs() {
    if (releaseDates == null || releaseDates.results() == null) {
      return null;
    }
    return releaseDates.results().stream()
        .filter(country -> "US".equals(country.isoCode()))
        .flatMap(country -> country.releaseDates().stream())
        .filter(entry -> entry.type() != null && entry.type() == 4)
        .map(entry -> parse(entry.releaseDate()))
        .filter(date -> date != null)
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  private static LocalDate parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      // TMDB's release_dates entries carry a full timestamp ("2021-07-16T00:00:00.000Z"); the
      // plain release_date field is already just a date.
      return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  // Each nested record needs its own @JsonIgnoreProperties — the annotation on the enclosing
  // TmdbMovieDetails record doesn't cascade to these, and TMDB's real response has several fields
  // (certification, descriptors, note, ...) none of these declare.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ReleaseDates(List<Country> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Country(
        @JsonProperty("iso_3166_1") String isoCode,
        @JsonProperty("release_dates") List<Entry> releaseDates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(@JsonProperty("release_date") String releaseDate, Integer type) {}
  }
}
