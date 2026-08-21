package de.oppahansi.kosmos.metadata.tmdb;

import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;

/**
 * {@code TmdbMovie}/{@code TmdbTvShow} → {@link MetadataSearchResult} mapping, shared by {@link
 * TmdbMetadataProvider} (search, detail-extras recommendations) and {@link TmdbDiscoverClient}
 * (trending/popular/upcoming/discover) — both need the same conversion, so it lives here rather
 * than being duplicated in each.
 */
final class TmdbMappers {

  private TmdbMappers() {}

  static MetadataSearchResult toSearchResult(TmdbMovie movie) {
    return new MetadataSearchResult(
        String.valueOf(movie.id()),
        movie.title(),
        extractYear(movie.releaseDate()),
        movie.overview(),
        movie.posterPath(),
        movie.backdropPath(),
        movie.voteAverage(),
        "movie");
  }

  static MetadataSearchResult toSearchResult(TmdbTvShow show) {
    return new MetadataSearchResult(
        String.valueOf(show.id()),
        show.name(),
        extractYear(show.firstAirDate()),
        show.overview(),
        show.posterPath(),
        show.backdropPath(),
        null,
        "tv");
  }

  static Integer extractYear(String releaseDate) {
    if (releaseDate == null || releaseDate.length() < 4) {
      return null;
    }
    return Integer.valueOf(releaseDate.substring(0, 4));
  }
}
