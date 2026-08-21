const TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p";

export type PosterSize = "w185" | "w342" | "w500";
export type BackdropSize = "w780" | "w1280" | "original";

function resolveUrl(path: string | null, prefixedSize: string): string | null {
  if (!path) return null;
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${TMDB_IMAGE_BASE}/${prefixedSize}${path}`;
}

/**
 * Builds a full poster URL from the path the API stores. Most providers (TMDB) store a relative
 * path that needs the TMDB image CDN prefix; AniList stores an already-absolute URL and is
 * returned as-is. Returns null if there's no poster.
 */
export function posterUrl(path: string | null, size: PosterSize = "w342"): string | null {
  return resolveUrl(path, size);
}

/**
 * Same idea as {@link posterUrl} but for the wide hero/backdrop image — TMDB's own {@code
 * backdrop_path}, or AniList's {@code bannerImage} (already absolute, passed through as-is).
 * Returns null if there's no backdrop, letting the caller fall back to the static gradient.
 */
export function backdropUrl(path: string | null, size: BackdropSize = "w1280"): string | null {
  return resolveUrl(path, size);
}

/** Studio/network logo image — always TMDB, always a relative path. */
export function logoUrl(path: string | null): string | null {
  return resolveUrl(path, "w300");
}

/** Cast member headshot — always TMDB, always a relative path; null for AniList (no cast data). */
export function profileUrl(path: string | null): string | null {
  return resolveUrl(path, "w185");
}
