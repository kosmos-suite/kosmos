package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * TMDB's {@code /tv/{id}?append_to_response=credits,recommendations,content_ratings} response — the
 * show-detail counterpart to {@link TmdbMovieDetailFull}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TmdbTvDetailFull(
    List<TmdbGenre> genres,
    @JsonProperty("first_air_date") String firstAirDate,
    @JsonProperty("vote_average") Double voteAverage,
    @JsonProperty("vote_count") Integer voteCount,
    List<Network> networks,
    @JsonProperty("created_by") List<Creator> createdBy,
    TmdbCredits credits,
    Recommendations recommendations,
    @JsonProperty("content_ratings") ContentRatings contentRatings,
    TmdbVideos videos) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Network(String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Creator(String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Recommendations(List<TmdbTvShow> results) {}

  /** Per-country content rating — Kosmos only ever reads the US entry. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record ContentRatings(List<Country> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Country(@JsonProperty("iso_3166_1") String isoCode, String rating) {}
  }
}
