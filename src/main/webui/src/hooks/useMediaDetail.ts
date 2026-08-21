import { useParams } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "./useApi";
import type { Anime, AnimeDetail, MediaDetailExtras, MediaPreview, Movie, Show, ShowDetail } from "../api/types";

export type MediaKind = "movie" | "show" | "anime";

/** The union every owned {@code Movie}/{@code ShowDetail}/{@code AnimeDetail} row shares. */
export type OwnedMedia = Movie | ShowDetail | AnimeDetail;

const MEDIA_TYPE_FOR_KIND: Record<MediaKind, "movie" | "tv" | "anime"> = {
  movie: "movie",
  show: "tv",
  anime: "anime",
};

/** {@code AddableItem.mediaType}/{@code CreateXRequest.mediaType} spelling for a given {@link MediaKind}. */
export function mediaTypeFor(kind: MediaKind): "movie" | "tv" | "anime" {
  return MEDIA_TYPE_FOR_KIND[kind];
}

function fetchOwned(kind: MediaKind, id: string): Promise<OwnedMedia> {
  if (kind === "movie") return api.getMovie(id);
  if (kind === "show") return api.getShow(id);
  return api.getAnime(id);
}

function fetchExtras(kind: MediaKind, id: string): Promise<MediaDetailExtras> {
  if (kind === "movie") return api.getMovieDetailExtras(id);
  if (kind === "show") return api.getShowDetailExtras(id);
  return api.getAnimeDetailExtras(id);
}

function fetchPreview(kind: MediaKind, externalId: string): Promise<MediaPreview> {
  if (kind === "movie") return api.getMoviePreview(externalId);
  if (kind === "show") return api.getShowPreview(externalId);
  return api.getAnimePreview(externalId);
}

function saveQualityProfile(kind: MediaKind, id: string, qualityProfileId: string | null): Promise<Movie | Show | Anime> {
  if (kind === "movie") return api.updateMovieQualityProfile(id, qualityProfileId);
  if (kind === "show") return api.updateShowQualityProfile(id, qualityProfileId);
  return api.updateAnimeQualityProfile(id, qualityProfileId);
}

/**
 * All the data a media detail page needs, for whichever of the three real content types the route
 * is for — one owned/preview/extras fetch dispatch instead of three near-identical page components.
 * {@code kind} is a stable prop for the lifetime of one mount (each of the six detail routes maps to
 * a distinct {@code <Route>}, so React remounts rather than reusing an instance across kinds), so
 * dispatching on it here doesn't risk a conditional hook call.
 */
export function useMediaDetail(kind: MediaKind) {
  const { id, externalId } = useParams<{ id?: string; externalId?: string }>();
  const owned = !!id;

  const { data: ownedMedia, loading: ownedLoading, error: ownedError, reload } = useApi(
    () => (id ? fetchOwned(kind, id) : Promise.resolve(null)),
    [kind, id],
  );
  const { data: extras } = useApi(() => (id ? fetchExtras(kind, id) : Promise.resolve(null)), [kind, id]);
  const { data: libraryFiles } = useApi(
    () => (kind === "movie" && id ? api.listMovieLibraryFiles(id) : Promise.resolve([])),
    [kind, id],
  );
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const {
    data: preview,
    loading: previewLoading,
    error: previewError,
  } = useApi(() => (externalId ? fetchPreview(kind, externalId) : Promise.resolve(null)), [kind, externalId]);

  async function setQualityProfile(qualityProfileId: string | null) {
    if (!id) return;
    await saveQualityProfile(kind, id, qualityProfileId);
    reload();
  }

  return {
    owned,
    ownedMedia,
    ownedLoading,
    ownedError,
    extras,
    libraryFiles: libraryFiles ?? [],
    profiles,
    preview,
    previewLoading,
    previewError,
    setQualityProfile,
  };
}
