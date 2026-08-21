import {
  CheckCircleIcon as CheckCircle,
  FilmStripIcon as FilmStrip,
  MinusCircleIcon as MinusCircle,
  PlusIcon as Plus,
  SpinnerIcon as Spinner,
} from "@phosphor-icons/react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { backdropUrl, posterUrl } from "../api/tmdbImage";
import type { AddState } from "../hooks/useAddToLibrary";

export type MediaStatus = "in-library" | "partially-available" | "downloading" | "missing";

const STATUS_LABEL: Record<MediaStatus, string> = {
  "in-library": "In Library",
  "partially-available": "Partially Available",
  downloading: "Downloading",
  missing: "Missing",
};

const STATUS_DOT_CLASS: Record<MediaStatus, string> = {
  "in-library": "dot-good",
  "partially-available": "dot-warn",
  downloading: "dot-warn",
  missing: "dot-bad",
};

const TYPE_LABEL: Record<"movie" | "tv" | "anime", string> = {
  movie: "Movie",
  tv: "Series",
  anime: "Anime",
};

interface MediaCardProps {
  to: string;
  title: string;
  year: number | null;
  posterPath: string | null;
  /** Only used when `wide` — the native 16:9 image shown instead of a cropped portrait poster. */
  backdropPath?: string | null;
  /** Renders in a 16:9 landscape frame using `backdropPath` instead of stretching the portrait
   * poster into one — used by wide rows like "Recently Added". */
  wide?: boolean;
  /** When TMDB has no poster/backdrop, tries the item's own media folder (sidecar image or, for
   * posters, embedded cover art) before falling back to the placeholder — only meaningful for
   * items already in the library, since only those have a folder on disk to check. */
  mediaItemId?: string | null;
  /** When present, switches to the Discover-grid card treatment: a Movie/Series/Anime type pill
   * top-left and a compact status icon top-right, instead of the plain text status pill every
   * other caller uses. */
  mediaType?: "movie" | "tv" | "anime";
  /** Omit for real library data with no known status yet — only pass for explicitly mocked rows. */
  status?: MediaStatus;
  /** Percent, 0-100. Renders a thin progress bar at the card's bottom edge when present. */
  progress?: number;
  /** Placeholder background for posterless mock cards — see utils/tonalGradient. */
  placeholderBackground?: string;
  /** When present and `status` isn't "in-library", a small "+" icon appears on hover (same top-right
   * slot the in-library check uses) to add/request the item directly from wherever the card is
   * shown — see useAddToLibrary. Clicking it never triggers the card's own navigation. */
  onAdd?: () => void;
  addState?: AddState;
  /** Extra class appended to the card's own root class — e.g. a keyboard-focus ring. */
  className?: string;
}

export function MediaCard({
  to,
  title,
  year,
  posterPath,
  backdropPath,
  wide,
  mediaItemId,
  mediaType,
  status,
  progress,
  placeholderBackground,
  onAdd,
  addState = "idle",
  className,
}: MediaCardProps) {
  const [localFailed, setLocalFailed] = useState(false);
  const tmdbSrc = wide ? backdropUrl(backdropPath ?? null) : posterUrl(posterPath);
  const localKind = wide ? "backdrop" : "poster";
  const localSrc = !tmdbSrc && mediaItemId && !localFailed ? `/api/media-items/${mediaItemId}/local-${localKind}` : null;
  const src = tmdbSrc ?? localSrc;
  const addLabel = addState === "adding" ? "Adding…" : addState === "added" ? "Added" : "Add";

  return (
    <Link to={to} className={`media-card${className ? ` ${className}` : ""}`}>
      <div className="media-card-art">
        {src ? (
          <img
            className="media-card-poster"
            src={src}
            alt=""
            loading="lazy"
            onError={localSrc ? () => setLocalFailed(true) : undefined}
          />
        ) : (
          <div className="media-card-placeholder" style={placeholderBackground ? { background: placeholderBackground } : undefined}>
            <FilmStrip size={28} />
          </div>
        )}

        {mediaType ? (
          <>
            <div className="media-card-type-badge">{TYPE_LABEL[mediaType]}</div>
            {status === "in-library" || status === "partially-available" ? (
              <div className="media-card-status-icon" title={STATUS_LABEL[status]}>
                {status === "in-library" ? (
                  <CheckCircle size={13} weight="fill" style={{ color: "var(--status-good)" }} />
                ) : (
                  <MinusCircle size={13} weight="fill" style={{ color: "var(--status-good)" }} />
                )}
              </div>
            ) : (
              onAdd && (
                <button
                  type="button"
                  className={`media-card-add-icon${addState !== "idle" ? " visible" : ""}`}
                  disabled={addState !== "idle"}
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    onAdd();
                  }}
                  title={addLabel}
                  aria-label={addLabel}
                >
                  {addState === "adding" ? (
                    <Spinner size={14} className="spin" />
                  ) : addState === "added" ? (
                    <CheckCircle size={14} weight="fill" />
                  ) : (
                    <Plus size={14} weight="bold" />
                  )}
                </button>
              )
            )}
          </>
        ) : (
          status && (
            <div className="media-card-badge">
              <span className={`dot ${STATUS_DOT_CLASS[status]}`} />
              {STATUS_LABEL[status]}
            </div>
          )
        )}

        <div className="media-card-scrim" />
        <div className="media-card-meta">
          <div className="media-card-title">{title}</div>
          {year && <div className="media-card-year">{year}</div>}
        </div>

        {progress != null && (
          <div className="media-card-progress">
            <div
              className={`media-card-progress-fill${status === "downloading" ? " downloading" : ""}`}
              style={{ width: `${progress}%` }}
            />
          </div>
        )}
      </div>
    </Link>
  );
}
