import type { DiscoverItem } from "../api/types";

/** Library detail page if already added, otherwise a search prefilled with the title. */
export function discoverItemLink(item: DiscoverItem): string {
  if (item.mediaItemId) {
    return item.mediaType === "tv" ? `/shows/${item.mediaItemId}` : `/movies/${item.mediaItemId}`;
  }
  return `/search?q=${encodeURIComponent(item.title)}`;
}
