package de.oppahansi.kosmos.metadata.dto;

import java.util.List;

/**
 * Supplementary detail-page content with no library-status concept of its own (cast, genres,
 * certification, recommendations) — fetched live from the item's metadata source (TMDB for
 * movies/shows, AniList for anime) rather than persisted, since it's display-only and can drift (a
 * cast member's credited name, a rating) without Kosmos needing to track it.
 */
public record MediaDetailExtras(
    List<String> genres,
    List<Fact> facts,
    Double voteAverage,
    Integer voteCount,
    String certification,
    List<CastMember> cast,
    List<MetadataSearchItem> similar,
    String trailerUrl,
    Collection collection) {

  /** Replaces {@code similar} once the caller has cross-referenced it against the library. */
  public MediaDetailExtras withSimilar(List<MetadataSearchItem> enrichedSimilar) {
    return new MediaDetailExtras(
        genres,
        facts,
        voteAverage,
        voteCount,
        certification,
        cast,
        enrichedSimilar,
        trailerUrl,
        collection);
  }

  /**
   * An ordered key/value pair for the Details panel — e.g. {@code {"Director", "Denis
   * Villeneuve"}}.
   */
  public record Fact(String k, String v) {}

  public record CastMember(String name, String role, String profilePath) {}

  /** The TMDB collection a movie belongs to, if any — null for shows/anime. */
  public record Collection(
      String externalId, String name, String posterPath, String backdropPath) {}
}
