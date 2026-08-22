package de.oppahansi.kosmos.importlists;

/**
 * A feed {@link ImportListService#sync} can pull candidates from. Every value today is TMDB —
 * {@link de.oppahansi.kosmos.metadata.tmdb.TmdbDiscoverClient} already fetches all of these for the
 * Discover page, so reusing it here costs no new integration work. Trakt lists, IMDb lists, and
 * AniList/MAL lists (the roadmap's fuller source set) are real future additions, not modeled yet —
 * adding one means a new enum value plus a new branch in {@link ImportListService#fetch}, not a
 * redesign.
 */
public enum ImportListSourceType {
  TMDB_POPULAR_MOVIES("movie"),
  TMDB_UPCOMING_MOVIES("movie"),
  TMDB_TRENDING_MOVIES("movie"),
  TMDB_POPULAR_TV("tv"),
  TMDB_UPCOMING_TV("tv"),
  TMDB_TRENDING_TV("tv");

  public final String mediaType;

  ImportListSourceType(String mediaType) {
    this.mediaType = mediaType;
  }
}
