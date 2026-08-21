import { FilmStripIcon as FilmStrip } from "@phosphor-icons/react";
import { Link } from "react-router-dom";
import { posterUrl } from "../api/tmdbImage";

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
  /** Omit for real library data with no known status yet — only pass for explicitly mocked rows. */
  status?: MovieStatus;
  /** Percent, 0-100. Renders a thin progress bar at the card's bottom edge when present. */
  progress?: number;
  /** Placeholder background for posterless mock cards — see utils/tonalGradient. */
  placeholderBackground?: string;
}

export function MovieCard({ to, title, year, posterPath, status, progress, placeholderBackground }: MovieCardProps) {
  const src = posterUrl(posterPath);

  return (
    <Link to={to} className="movie-card">
      <div className="movie-card-art">
        {src ? (
          <img className="movie-card-poster" src={src} alt="" loading="lazy" />
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
