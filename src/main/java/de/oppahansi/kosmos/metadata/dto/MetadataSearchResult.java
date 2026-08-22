package de.oppahansi.kosmos.metadata.dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * A single search result from a {@link de.oppahansi.kosmos.metadata.MetadataProvider}. {@code
 * mediaType} is "movie" or "tv" — which /movies or /shows endpoint to POST to next. {@code
 * backdropPath} rides along so a movie/show/anime created from this result can store it for its own
 * detail-page hero — see {@code media.Movie#backdropPath} and friends. {@code voteAverage} is only
 * ever populated for movie results today (TMDB's 0-10 scale) — it exists here rather than only on
 * {@code DiscoverItem} because {@code DiscoverService} builds trending/popular rows directly from
 * this record. {@code episodeCount} is only ever populated for anime results (AniList's own episode
 * total) — used by the "Needs Review" screen to help tell two same-named titles apart when
 * poster/year alone don't settle it; TMDB movie/tv results leave it null. {@code releaseDate} is
 * only ever populated for TMDB movie results (raw {@code YYYY-MM-DD}) — shows/anime have no single
 * release date the way a movie does; their calendar entries come from per-episode air dates
 * instead. Lets {@code MovieService}/{@code JellyfinSyncService} persist {@code Movie#releaseDate}
 * without a second TMDB call, since both already fetch this record.
 */
public record MetadataSearchResult(
    String externalId,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    Double voteAverage,
    String mediaType,
    Integer episodeCount,
    String releaseDate) {

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
