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
 * gets, wherever it's shown (Discover rows/grids, Search, the media detail page) — admins create
 * the title directly, other users file a request. Doesn't navigate anywhere itself: this backs an
 * in-place quick-add on a tile the user is browsing as much as a dedicated "add" flow, so the card
 * just flips to a checkmark and the user stays where they were by default. {@link triggerAdd}
 * resolves to the created row's id (admin path only — a filed request has no browsable page of its
 * own) so a caller that _does_ want to navigate after adding, like the detail page, can.
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

  async function triggerAdd(item: AddableItem): Promise<string | null> {
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
      let createdId: string | null = null;
      if (!admin) {
        await api.createRequest({ ...shared, mediaType: item.mediaType });
      } else if (item.mediaType === "tv") {
        createdId = (await api.createShow(shared)).id;
      } else if (item.mediaType === "anime") {
        createdId = (await api.createAnime(shared)).id;
      } else {
        createdId = (await api.createMovie(shared)).id;
      }
      setDoneIds((s) => new Set(s).add(item.externalId));
      return createdId;
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      return null;
    } finally {
      setAddingId(null);
    }
  }

  return { admin, stateFor, triggerAdd, error };
}
