import { CaretDownIcon as CaretDown, CaretUpIcon as CaretUp, MagnifyingGlassIcon as MagnifyingGlass } from "@phosphor-icons/react";
import { useState } from "react";
import { Link } from "react-router-dom";
import type { EpisodeStatus } from "../../api/types";

const STATUS_DOT: Record<EpisodeStatus, string> = {
  MISSING: "dot-bad",
  GRABBED: "dot-warn",
  IMPORTED: "dot-warn",
  AVAILABLE: "dot-good",
};
const STATUS_LABEL: Record<EpisodeStatus, string> = {
  MISSING: "Missing",
  GRABBED: "Grabbed",
  IMPORTED: "Importing",
  AVAILABLE: "Available",
};

/** One row shared by the grouped (show, inside a season) and flat (anime) episode lists below. */
export interface EpisodeRowData {
  id: string;
  number: number | null;
  title: string;
  airDate: string | null;
  status: EpisodeStatus;
  searchHref: string | null;
}

function EpisodeRow({ episode, borderBottom }: { episode: EpisodeRowData; borderBottom?: boolean }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "48px 1fr auto 90px 32px",
        gap: 14,
        alignItems: "center",
        padding: "8px 4px",
        fontSize: 12.5,
        borderBottom: borderBottom ? "1px solid var(--border)" : undefined,
      }}
    >
      <span className="text-faint" style={{ fontFamily: "var(--font-mono)" }}>
        {episode.number != null ? String(episode.number).padStart(2, "0") : "—"}
      </span>
      <span>{episode.title}</span>
      <span className="status-tag" style={{ fontSize: 10.5 }}>
        <span className={`dot ${STATUS_DOT[episode.status]}`} />
        {STATUS_LABEL[episode.status]}
      </span>
      <span className="text-faint" style={{ fontSize: 11, textAlign: "right" }}>
        {episode.airDate ?? "—"}
      </span>
      {episode.searchHref ? (
        <Link to={episode.searchHref} className="btn btn-icon" title="Interactive search" style={{ width: 26, height: 26 }}>
          <MagnifyingGlass size={12} />
        </Link>
      ) : (
        <span />
      )}
    </div>
  );
}

/** Anime's flat, ungrouped episode list. */
export function FlatEpisodeList({ episodes }: { episodes: EpisodeRowData[] }) {
  return (
    <>
      {episodes.map((ep) => (
        <EpisodeRow key={ep.id} episode={ep} borderBottom />
      ))}
    </>
  );
}

export interface SeasonRowData {
  id: string;
  name: string;
  episodeCount: number | null;
  episodes: EpisodeRowData[];
}

function SeasonRow({ season, open, onToggle }: { season: SeasonRowData; open: boolean; onToggle: () => void }) {
  return (
    <div style={{ borderBottom: "1px solid var(--border)" }}>
      <div onClick={onToggle} style={{ display: "flex", alignItems: "center", gap: 12, padding: "13px 4px", cursor: "pointer" }}>
        <span style={{ fontWeight: 500, fontSize: 13.5, flex: 1 }}>{season.name}</span>
        <span className="text-faint" style={{ fontSize: 11.5 }}>
          {season.episodeCount ?? season.episodes.length} episodes
        </span>
        {open ? <CaretUp size={13} className="text-faint" /> : <CaretDown size={13} className="text-faint" />}
      </div>

      {open && (
        <div style={{ paddingBottom: 12 }}>
          {season.episodes.map((ep) => (
            <EpisodeRow key={ep.id} episode={ep} />
          ))}
          {season.episodes.length === 0 && (
            <p className="text-muted" style={{ fontSize: 12, padding: "4px" }}>
              No episode data.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

/** A show's Season > Episode grouping, each season collapsed until clicked. */
export function GroupedEpisodeList({ seasons }: { seasons: SeasonRowData[] }) {
  const [openSeasonId, setOpenSeasonId] = useState<string | null>(null);
  return (
    <>
      {seasons.map((season) => (
        <SeasonRow
          key={season.id}
          season={season}
          open={openSeasonId === season.id}
          onToggle={() => setOpenSeasonId((s) => (s === season.id ? null : season.id))}
        />
      ))}
    </>
  );
}

