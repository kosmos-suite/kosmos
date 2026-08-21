import type { DiscoverItem } from "../api/types";

/**
 * Drops items already seen by identity ({@code mediaType}+{@code externalId}, the only stable key
 * a TMDB result has) — first occurrence wins. Needed because TMDB's popularity-sorted lists aren't
 * perfectly stable across separate requests, so infinite-scroll pagination can re-surface the same
 * title on a later page even though the backend already dedupes within a single page's response.
 */
export function dedupeDiscoverItems(items: DiscoverItem[]): DiscoverItem[] {
  const seen = new Set<string>();
  const out: DiscoverItem[] = [];
  for (const item of items) {
    // externalId is null for library-native rows (e.g. Recently Added) that never flow through
    // TMDB pagination in the first place — mediaItemId is the stable key there instead.
    const key = item.externalId ? `${item.mediaType}:${item.externalId}` : `id:${item.mediaItemId}`;
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(item);
  }
  return out;
}
