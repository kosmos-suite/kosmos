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
    List<MetadataSearchResult> similar) {

  /**
   * An ordered key/value pair for the Details panel — e.g. {@code {"Director", "Denis
   * Villeneuve"}}.
   */
  public record Fact(String k, String v) {}

  public record CastMember(String name, String role, String profilePath) {}
}
