import {
  CheckCircleIcon as CheckCircle,
  PlusIcon as Plus,
  SpinnerIcon as Spinner,
  StarIcon as Star,
} from "@phosphor-icons/react";
import { useParams } from "react-router-dom";
import { backdropUrl, posterUrl } from "../api/tmdbImage";
import type { MediaPreview } from "../api/types";
import { CastRow, SimilarRow } from "../components/DetailExtrasSections";
import { useAddToLibrary } from "../hooks/useAddToLibrary";
import { useApi } from "../hooks/useApi";

interface MediaPreviewPageProps {
  mediaType: "movie" | "tv" | "anime";
  fetchPreview: (externalId: string) => Promise<MediaPreview>;
}

/**
 * The detail screen for a title Kosmos doesn't own yet — reached from a not-in-library card
 * anywhere in the app (Discover, Search, More Like This) instead of falling back to a search.
 * Shares Cast/Similar with the owned detail pages; everything else (quality profile, root folder,
 * file/season status) doesn't apply yet since there's no real {@code Movie}/{@code Show}/{@code
 * Anime} row until Add/Request is clicked.
 */
export default function MediaPreviewPage({ mediaType, fetchPreview }: MediaPreviewPageProps) {
  const { externalId } = useParams<{ externalId: string }>();
  const { data: preview, loading, error } = useApi(() => fetchPreview(externalId!), [externalId]);
  const { admin, stateFor, triggerAdd } = useAddToLibrary();

  if (loading) return <div className="page">Loading…</div>;
  if (error) return <div className="page text-muted">Failed to load: {error}</div>;
  if (!preview) return null;

  const state = stateFor(preview.externalId);
  const label =
    state === "adding"
      ? admin
        ? "Adding…"
        : "Requesting…"
      : state === "added"
        ? admin
          ? "Added"
          : "Requested"
        : admin
          ? "Add to Library"
          : "Request";

  const backdrop = backdropUrl(preview.backdropPath);
  const poster = posterUrl(preview.posterPath, "w500");

  return (
    <div>
      <section
        className="detail-hero"
        style={backdrop ? { backgroundImage: `url(${backdrop})`, backgroundSize: "cover", backgroundPosition: "center" } : undefined}
      />

      <div className="detail-body2">
        <div className="detail-poster2">
          <div className="detail-poster2-art">{poster && <img src={poster} alt="" />}</div>
        </div>

        <div className="detail-body2-main">
          <div className="detail-title-row">
            <span className="status-pill" style={{ background: "rgba(145,132,217,.18)", color: "#D2CEFD" }}>
              Not in Library
            </span>
          </div>

          <h1 className="detail-h1">{preview.title}</h1>

          <div className="detail-meta-row2">
            {preview.year && <span>{preview.year}</span>}
            {preview.certification && (
              <>
                <span className="sep" />
                <span className="cert-badge">{preview.certification}</span>
              </>
            )}
            {preview.voteAverage != null && (
              <>
                <span className="sep" />
                <span style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Star size={12} weight="fill" color="#E0A94A" />
                  {preview.voteAverage.toFixed(1)} <span className="text-ghost">TMDB</span>
                </span>
              </>
            )}
          </div>

          {preview.genres.length > 0 && (
            <div className="detail-genres">
              {preview.genres.map((g) => (
                <span key={g} className="genre-tag">
                  {g}
                </span>
              ))}
            </div>
          )}

          {preview.overview && (
            <p className="detail-synopsis" style={{ maxWidth: "70ch" }}>
              {preview.overview}
            </p>
          )}

          <button
            type="button"
            className="btn btn-hero"
            style={{ marginTop: 14 }}
            disabled={state !== "idle"}
            onClick={() =>
              triggerAdd({
                externalId: preview.externalId,
                title: preview.title,
                year: preview.year,
                overview: preview.overview,
                posterPath: preview.posterPath,
                backdropPath: preview.backdropPath,
                mediaType,
              })
            }
          >
            {state === "adding" ? (
              <Spinner size={16} className="spin" />
            ) : state === "added" ? (
              <CheckCircle size={16} weight="fill" />
            ) : (
              <Plus size={16} weight="bold" />
            )}
            {label}
          </button>
        </div>
      </div>

      {preview.facts.length > 0 && (
        <div className="page">
          <div style={{ maxWidth: 420 }}>
            <div className="section-label">Details</div>
            <div className="fact-list">
              {preview.facts.map((f) => (
                <div key={f.k} className="fact-list-row">
                  <span className="k">{f.k}</span>
                  <span className="v">{f.v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      <CastRow cast={preview.cast} />
      <SimilarRow items={preview.similar} />
    </div>
  );
}
