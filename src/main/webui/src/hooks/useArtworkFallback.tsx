import { useState } from "react";

/**
 * Local-media-folder fallback for when TMDB has no poster/backdrop for a library item — tries
 * `/api/media-items/{id}/local-{kind}` (see `LocalArtworkService` on the backend) once `tmdbUrl` is
 * null, and falls back further to `null` (letting the caller render its own placeholder) if that
 * 404s too. `probe` must be rendered somewhere in the tree — it's an invisible `<img>` whose only
 * job is to catch the local URL's load failure, since a CSS `background-image` never fires `onError`
 * itself.
 */
export function useArtworkFallback(
  tmdbUrl: string | null,
  mediaItemId: string | null | undefined,
  kind: "poster" | "backdrop",
): { url: string | null; probe: React.ReactElement | null } {
  const [failed, setFailed] = useState(false);
  const localUrl = !tmdbUrl && mediaItemId && !failed ? `/api/media-items/${mediaItemId}/local-${kind}` : null;
  const url = tmdbUrl ?? localUrl;
  const probe = localUrl ? <img src={localUrl} alt="" style={{ display: "none" }} onError={() => setFailed(true)} /> : null;
  return { url, probe };
}
