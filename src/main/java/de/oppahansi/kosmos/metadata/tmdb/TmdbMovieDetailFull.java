package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * TMDB's {@code /movie/{id}?append_to_response=credits,recommendations,release_dates} response —
 * one HTTP call in place of four, backing the movie detail page's Cast/Similar/Details sections.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbMovieDetailFull(
    List<TmdbGenre> genres,
    @JsonProperty("release_date") String releaseDate,
    @JsonProperty("vote_average") Double voteAverage,
    @JsonProperty("vote_count") Integer voteCount,
    @JsonProperty("production_companies") List<Company> productionCompanies,
    TmdbCredits credits,
    Recommendations recommendations,
    @JsonProperty("release_dates") ReleaseDates releaseDates,
    TmdbVideos videos,
    @JsonProperty("belongs_to_collection") Collection belongsToCollection) {

  /** Null when the movie isn't part of a TMDB collection. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Collection(
      int id,
      String name,
      @JsonProperty("poster_path") String posterPath,
      @JsonProperty("backdrop_path") String backdropPath) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Company(String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Recommendations(List<TmdbMovie> results) {}

  /** Per-country release/certification info — Kosmos only ever reads the US entry. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record ReleaseDates(List<Country> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Country(
        @JsonProperty("iso_3166_1") String isoCode,
        @JsonProperty("release_dates") List<Certification> releaseDates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Certification(String certification) {}
  }
}
