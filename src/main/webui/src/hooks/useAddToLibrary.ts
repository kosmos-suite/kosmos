import { useState } from "react";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export type AddState = "idle" | "adding" | "added";

export interface AddableItem {
  externalId: string;
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  mediaType: "movie" | "tv" | "anime";
}

/**
 * Shared add-to-library/request logic behind the hover "+" icon every not-yet-owned media tile
 * gets, wherever it's shown (Discover rows/grids, Search) — admins create the title directly, other
 * users file a request. Deliberately doesn't navigate anywhere on success: this backs an in-place
 * quick-add on a tile the user is browsing, not a dedicated "add" flow, so the card just flips to a
 * checkmark and the user stays where they were.
 */
export function useAddToLibrary() {
  const { user } = useAuth();
  const admin = user?.role === "ADMIN";
  const [addingId, setAddingId] = useState<string | null>(null);
  const [doneIds, setDoneIds] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);

  function stateFor(externalId: string): AddState {
    if (addingId === externalId) return "adding";
    if (doneIds.has(externalId)) return "added";
    return "idle";
  }

  async function triggerAdd(item: AddableItem) {
    setAddingId(item.externalId);
    setError(null);
    const pluginSlug = item.mediaType === "anime" ? "anilist" : "tmdb";
    const shared = {
      externalId: item.externalId,
      pluginSlug,
      title: item.title,
      year: item.year,
      overview: item.overview,
      posterPath: item.posterPath,
      backdropPath: item.backdropPath,
    };
    try {
      if (!admin) {
        await api.createRequest({ ...shared, mediaType: item.mediaType });
      } else if (item.mediaType === "tv") {
        await api.createShow(shared);
      } else if (item.mediaType === "anime") {
        await api.createAnime(shared);
      } else {
        await api.createMovie(shared);
      }
      setDoneIds((s) => new Set(s).add(item.externalId));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setAddingId(null);
    }
  }

  return { admin, stateFor, triggerAdd, error };
}
