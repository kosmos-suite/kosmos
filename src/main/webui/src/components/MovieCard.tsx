import { FilmStripIcon as FilmStrip } from "@phosphor-icons/react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { backdropUrl, posterUrl } from "../api/tmdbImage";

export type MovieStatus = "in-library" | "downloading" | "missing";

const STATUS_LABEL: Record<MovieStatus, string> = {
  "in-library": "In Library",
  downloading: "Downloading",
  missing: "Missing",
};

const STATUS_DOT_CLASS: Record<MovieStatus, string> = {
  "in-library": "dot-good",
  downloading: "dot-warn",
  missing: "dot-bad",
};

interface MovieCardProps {
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
  /** Omit for real library data with no known status yet — only pass for explicitly mocked rows. */
  status?: MovieStatus;
  /** Percent, 0-100. Renders a thin progress bar at the card's bottom edge when present. */
  progress?: number;
  /** Placeholder background for posterless mock cards — see utils/tonalGradient. */
  placeholderBackground?: string;
}

export function MovieCard({
  to,
  title,
  year,
  posterPath,
  backdropPath,
  wide,
  mediaItemId,
  status,
  progress,
  placeholderBackground,
}: MovieCardProps) {
  const [localFailed, setLocalFailed] = useState(false);
  const tmdbSrc = wide ? backdropUrl(backdropPath ?? null) : posterUrl(posterPath);
  const localKind = wide ? "backdrop" : "poster";
  const localSrc = !tmdbSrc && mediaItemId && !localFailed ? `/api/media-items/${mediaItemId}/local-${localKind}` : null;
  const src = tmdbSrc ?? localSrc;

  return (
    <Link to={to} className="movie-card">
      <div className="movie-card-art">
        {src ? (
          <img
            className="movie-card-poster"
            src={src}
            alt=""
            loading="lazy"
            onError={localSrc ? () => setLocalFailed(true) : undefined}
          />
        ) : (
          <div className="movie-card-placeholder" style={placeholderBackground ? { background: placeholderBackground } : undefined}>
            <FilmStrip size={28} />
          </div>
        )}

        {status && (
          <div className="movie-card-badge">
            <span className={`dot ${STATUS_DOT_CLASS[status]}`} />
            {STATUS_LABEL[status]}
          </div>
        )}

        <div className="movie-card-scrim" />
        <div className="movie-card-meta">
          <div className="movie-card-title">{title}</div>
          {year && <div className="movie-card-year">{year}</div>}
        </div>

        {progress != null && (
          <div className="movie-card-progress">
            <div
              className={`movie-card-progress-fill${status === "downloading" ? " downloading" : ""}`}
              style={{ width: `${progress}%` }}
            />
          </div>
        )}
      </div>
    </Link>
  );
}
