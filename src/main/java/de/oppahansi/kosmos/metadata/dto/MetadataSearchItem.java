package de.oppahansi.kosmos.metadata.dto;

import java.util.UUID;

/**
 * Backs Search's real results — {@link MetadataSearchResult} plus whether it's already in the
 * library, the same cross-reference {@code media.DiscoverService} does for discover rows. Kept
 * separate from {@link MetadataSearchResult} itself rather than adding these fields there, since
 * that record backs several other flows (discover rows, recommendations, sync backfill) that have
 * no library-status concept of their own.
 */
public record MetadataSearchItem(
    UUID mediaItemId,
    String externalId,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    Double voteAverage,
    String mediaType,
    Integer episodeCount,
    boolean inLibrary,
    boolean partiallyAvailable) {

  /**
   * A result with no library-status lookup performed yet — used by providers (TMDB/AniList
   * recommendations) that have no DB access of their own. The owning {@code media.*Service}
   * replaces these placeholders with the real cross-referenced values before returning them.
   */
  public static MetadataSearchItem unenriched(MetadataSearchResult r) {
    return new MetadataSearchItem(
        null,
        r.externalId(),
        r.title(),
        r.year(),
        r.overview(),
        r.posterPath(),
        r.backdropPath(),
        r.voteAverage(),
        r.mediaType(),
        r.episodeCount(),
        false,
        false);
  }
}
