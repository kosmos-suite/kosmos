import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import type { SeasonPassEntry } from "../api/types";
import { posterUrl } from "../api/tmdbImage";

function badgeClass(have: number, total: number): string {
  if (total === 0 || have === total) return "dot-good";
  if (have === 0) return "dot-bad";
  return "dot-warn";
}

function ownedPath(entry: SeasonPassEntry): string {
  return entry.contentType === "show" ? `/shows/${entry.mediaItemId}` : `/anime/${entry.mediaItemId}`;
}

export default function SeasonPassPage() {
  const { data: entries, error } = useApi(() => api.getSeasonPass(), []);

  return (
    <div className="page with-top-padding">
      <div className="page-header">
        <div style={{ flex: 1, minWidth: 280 }}>
          <h1 style={{ marginBottom: 6 }}>Season Pass</h1>
          <p className="text-muted" style={{ maxWidth: "60ch" }}>
            Every show and anime's episode completeness, season by season.
          </p>
        </div>
      </div>

      {error && <p className="text-muted">Failed to load — {error}</p>}
      {entries?.length === 0 && <p className="text-muted">Nothing in the library yet.</p>}

      {entries?.map((entry) => (
        <Link key={entry.mediaItemId} to={ownedPath(entry)} className="indexer-row">
          <span
            className="poster-thumb sm"
            style={
              posterUrl(entry.posterPath)
                ? { backgroundImage: `url(${posterUrl(entry.posterPath)})`, backgroundSize: "cover" }
                : undefined
            }
          />
          <div className="indexer-row-main">
            <div className="indexer-row-title-line">
              <span className="indexer-row-name">{entry.title}</span>
              <span className="protocol-pill">{entry.contentType === "show" ? "Series" : "Anime"}</span>
            </div>
            <div className="indexer-row-sub" style={{ flexWrap: "wrap" }}>
              {entry.seasons.length === 0 && <span className="text-faint">No seasons yet</span>}
              {entry.seasons.map((s) => (
                <span key={s.seasonNumber} style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <span className={`dot ${badgeClass(s.haveCount, s.totalCount)}`} />
                  {`S${s.seasonNumber} ${s.haveCount}/${s.totalCount}`}
                </span>
              ))}
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
}
