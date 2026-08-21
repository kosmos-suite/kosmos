import type { DiscoverItem } from "../api/types";

/**
 * Library detail page if already added, otherwise a preview screen for the TMDB title — same
 * Add/Request flow as the owned pages, just without a real library row yet.
 */
export function discoverItemLink(item: DiscoverItem): string {
  if (item.mediaItemId) {
    return item.mediaType === "tv" ? `/shows/${item.mediaItemId}` : `/movies/${item.mediaItemId}`;
  }
  if (!item.externalId) {
    return `/search?q=${encodeURIComponent(item.title)}`;
  }
  return item.mediaType === "tv" ? `/shows/tmdb/${item.externalId}` : `/movies/tmdb/${item.externalId}`;
}
