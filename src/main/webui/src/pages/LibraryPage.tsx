import {
  CalendarBlankIcon as CalendarBlank,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  ClockIcon as Clock,
  DotsThreeIcon as DotsThree,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  FilmStripIcon as FilmStrip,
  ListBulletsIcon as ListBullets,
  MagnifyingGlassIcon as MagnifyingGlass,
  MedalIcon as Medal,
  PlanetIcon as Planet,
  SortAscendingIcon as SortAscending,
  SquaresFourIcon as SquaresFour,
  TextAaIcon as TextAa,
  TrashIcon as Trash,
} from "@phosphor-icons/react";
import { useMemo, useState, type CSSProperties } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import type { Movie } from "../api/types";
import { posterUrl } from "../api/tmdbImage";
import { useApi } from "../hooks/useApi";
import { tonalGradient } from "../utils/tonalGradient";

type View = "grid" | "list";
type SortKey = "added" | "title" | "year" | "quality";
type LibraryStatus = "in-library" | "downloading" | "missing";

/**
 * The real Movie API (api/types.ts) has no file-size or monitoring/download-status fields yet.
 * Every movie that exists in the DB was added via the metadata-search flow, so it's always shown
 * as "in-library"; "downloading"/"missing" stay in the type and the row/card rendering for design
 * fidelity, but no real row can currently produce them.
 */
interface LibraryItem {
  id: string;
  title: string;
  year: number | null;
  posterPath: string | null;
  quality: string;
  sizeGb: number | null;
  addedDaysAgo: number;
  status: LibraryStatus;
  progress?: number;
  tone: number;
}

function hashTone(id: string): number {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) | 0;
  return Math.abs(h);
}

function toLibraryItem(movie: Movie): LibraryItem {
  const addedMs = Date.now() - new Date(movie.addedAt).getTime();
  return {
    id: movie.id,
    title: movie.title,
    year: movie.year,
    posterPath: movie.posterPath,
    quality: "—",
    sizeGb: null,
    addedDaysAgo: Math.max(0, Math.floor(addedMs / (1000 * 60 * 60 * 24))),
    status: "in-library",
    tone: hashTone(movie.id),
  };
}

const SORTS: { key: SortKey; label: string; icon: typeof Clock }[] = [
  { key: "added", label: "Added date", icon: Clock },
  { key: "title", label: "Title", icon: TextAa },
  { key: "year", label: "Year", icon: CalendarBlank },
  { key: "quality", label: "Quality", icon: Medal },
];

const FILTERS: { status: LibraryStatus; label: string; dotClass: string }[] = [
  { status: "missing", label: "Missing", dotClass: "dot-bad" },
  { status: "in-library", label: "Downloaded", dotClass: "dot-good" },
  { status: "downloading", label: "Downloading", dotClass: "dot-warn" },
];

const STATUS_META: Record<LibraryStatus, { label: string; dotClass: string; tagClass: string }> = {
  "in-library": { label: "In Library", dotClass: "dot-good", tagClass: "lib-good" },
  downloading: { label: "Downloading", dotClass: "dot-warn", tagClass: "lib-warn" },
  missing: { label: "Missing", dotClass: "dot-bad", tagClass: "lib-bad" },
};

function qualityRank(quality: string): number {
  if (quality.startsWith("2160p Remux")) return 4;
  if (quality.startsWith("2160p")) return 3;
  if (quality.startsWith("1080p")) return 2;
  if (quality === "—") return -1;
  return 0;
}

function LibraryGridCard({ item }: { item: LibraryItem }) {
  const isMissing = item.status === "missing";
  const isDownloading = item.status === "downloading";
  const meta = STATUS_META[item.status];
  const [localFailed, setLocalFailed] = useState(false);
  const tmdbSrc = posterUrl(item.posterPath);
  const localSrc = !tmdbSrc && !localFailed ? `/api/media-items/${item.id}/local-poster` : null;
  const src = tmdbSrc ?? localSrc;

  return (
    <Link to={`/movies/${item.id}`} className="movie-card">
      <div className={`movie-card-art${isMissing ? " missing" : ""}`}>
        {src ? (
          <img
            className="movie-card-poster"
            src={src}
            alt=""
            loading="lazy"
            onError={localSrc ? () => setLocalFailed(true) : undefined}
          />
        ) : (
          <div className="movie-card-placeholder" style={{ background: tonalGradient(item.tone) }}>
            <FilmStrip size={28} />
          </div>
        )}

        {isDownloading ? (
          <div
            className="movie-card-ring"
            style={{ "--pct": `${item.progress ?? 0}%` } as CSSProperties}
          >
            {item.progress}%
          </div>
        ) : (
          <div className="movie-card-badge">
            <span className={`dot ${meta.dotClass}`} />
            {meta.label}
          </div>
        )}

        <div className="movie-card-scrim" />
        <div className="movie-card-meta">
          <div className="movie-card-title">{item.title}</div>
          {item.year && <div className="movie-card-year">{item.year}</div>}
        </div>

        {isDownloading && (
          <div className="movie-card-progress">
            <div className="movie-card-progress-fill downloading" style={{ width: `${item.progress}%` }} />
          </div>
        )}
      </div>
    </Link>
  );
}

function LibraryListRow({ item }: { item: LibraryItem }) {
  const meta = STATUS_META[item.status];
  const src = posterUrl(item.posterPath);
  const subLabel =
    item.status === "in-library"
      ? "on disk"
      : item.status === "downloading"
        ? `grabbing from indexer · ${item.progress}%`
        : "monitored · no release found";

  return (
    <Link to={`/movies/${item.id}`} className="library-list-row">
      <div className="library-list-title-cell">
        {src ? (
          <img className="library-list-thumb" src={src} alt="" loading="lazy" />
        ) : (
          <div className="library-list-thumb" style={{ background: tonalGradient(item.tone) }} />
        )}
        <div className="library-list-title-main">
          <div className="title">{item.title}</div>
          <div className="sub">{subLabel}</div>
        </div>
      </div>
      <span className="library-list-mono">{item.year ?? "—"}</span>
      <span className={`library-list-mono${item.quality === "—" ? " dim" : ""}`}>{item.quality}</span>
      <span className="library-list-mono">{item.sizeGb != null ? `${item.sizeGb.toFixed(1)} GB` : "—"}</span>
      <span className={`status-tag ${meta.tagClass}`}>
        <span className={`dot ${meta.dotClass}`} />
        {meta.label}
      </span>
      <div className="library-list-actions">
        <button type="button" className="btn btn-icon" onClick={(e) => e.preventDefault()} title="Search">
          <MagnifyingGlass size={14} />
        </button>
        <button type="button" className="btn btn-icon" onClick={(e) => e.preventDefault()} title="Remove">
          <Trash size={14} />
        </button>
        <button type="button" className="btn btn-icon" onClick={(e) => e.preventDefault()} title="More">
          <DotsThree size={16} />
        </button>
      </div>
    </Link>
  );
}

function EmptyLibrary() {
  return (
    <div className="empty-state">
      <div className="empty-state-inner">
        <div style={{ position: "relative", width: 150, height: 112, margin: "0 auto 30px" }}>
          <div
            style={{
              position: "absolute",
              left: 6,
              top: 12,
              width: 58,
              aspectRatio: "2 / 3",
              borderRadius: 10,
              background: "#14151f",
              border: "1px dashed var(--border-strong)",
              transform: "rotate(-8deg)",
            }}
          />
          <div
            style={{
              position: "absolute",
              right: 6,
              top: 12,
              width: 58,
              aspectRatio: "2 / 3",
              borderRadius: 10,
              background: "#14151f",
              border: "1px dashed var(--border-strong)",
              transform: "rotate(8deg)",
            }}
          />
          <div
            style={{
              position: "absolute",
              left: "50%",
              top: 0,
              transform: "translateX(-50%)",
              width: 62,
              aspectRatio: "2 / 3",
              borderRadius: 11,
              background: "var(--bg)",
              border: "1px solid rgba(145,132,217,.28)",
              display: "grid",
              placeItems: "center",
              boxShadow: "0 16px 36px rgba(0,0,0,.5)",
            }}
          >
            <Planet size={22} color="#b5abfc" />
          </div>
        </div>
        <div className="empty-state-title">Your library is empty</div>
        <p className="empty-state-body">
          Add a movie, series, or anime from Discover or Search and it'll show up here once it's on disk.
        </p>
        <div className="empty-state-actions">
          <Link to="/" className="btn btn-hero">
            <SquaresFour size={15} />
            Discover Movies
          </Link>
          <button type="button" className="btn btn-secondary" disabled title="Coming soon">
            Scan a folder
          </button>
        </div>
        <p className="text-faint" style={{ marginTop: 20, fontSize: 12 }}>
          Coming from Radarr? <strong>Import your existing library</strong> instead.
        </p>
      </div>
    </div>
  );
}

export default function LibraryPage() {
  const { data: movies, loading, error: loadError } = useApi(() => api.listMovies(), []);
  const [view, setView] = useState<View>("grid");
  const [previewEmpty, setPreviewEmpty] = useState(false);
  const [filterText, setFilterText] = useState("");
  const [statusFilters, setStatusFilters] = useState<Set<LibraryStatus>>(new Set());
  const [sort, setSort] = useState<SortKey>("added");
  const [sortOpen, setSortOpen] = useState(false);

  const libraryMovies = useMemo(() => (movies ?? []).map(toLibraryItem), [movies]);

  const counts: Record<LibraryStatus, number> = {
    "in-library": libraryMovies.filter((m) => m.status === "in-library").length,
    downloading: libraryMovies.filter((m) => m.status === "downloading").length,
    missing: libraryMovies.filter((m) => m.status === "missing").length,
  };

  const filtered = useMemo(() => {
    let rows = libraryMovies;
    if (filterText.trim()) {
      const q = filterText.trim().toLowerCase();
      rows = rows.filter((m) => m.title.toLowerCase().includes(q));
    }
    if (statusFilters.size > 0) {
      rows = rows.filter((m) => statusFilters.has(m.status));
    }
    const sorted = [...rows];
    switch (sort) {
      case "title":
        sorted.sort((a, b) => a.title.localeCompare(b.title));
        break;
      case "year":
        sorted.sort((a, b) => (b.year ?? 0) - (a.year ?? 0));
        break;
      case "quality":
        sorted.sort((a, b) => qualityRank(b.quality) - qualityRank(a.quality));
        break;
      default:
        sorted.sort((a, b) => a.addedDaysAgo - b.addedDaysAgo);
    }
    return sorted;
  }, [libraryMovies, filterText, statusFilters, sort]);

  const isFiltered = filterText.trim() !== "" || statusFilters.size > 0;
  const showEmpty =
    !loading && (previewEmpty || (isFiltered ? filtered.length === 0 : libraryMovies.length === 0));

  function toggleFilter(status: LibraryStatus) {
    setStatusFilters((prev) => {
      const next = new Set(prev);
      if (next.has(status)) next.delete(status);
      else next.add(status);
      return next;
    });
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Movies</h1>
        <span className="page-count">
          {isFiltered ? `${filtered.length} of ${libraryMovies.length} movies` : `${libraryMovies.length} movies`}
        </span>
      </div>

      {loadError && <p className="text-muted">Failed to load library: {loadError}</p>}

      <div className="library-toolbar">
        <input
          className="input library-filter-input"
          value={filterText}
          onChange={(e) => setFilterText(e.target.value)}
          placeholder="Filter this library…"
        />

        {FILTERS.map((f) => (
          <button
            key={f.status}
            type="button"
            className={`chip-filter${statusFilters.has(f.status) ? " active" : ""}`}
            onClick={() => toggleFilter(f.status)}
          >
            <span className={`dot ${f.dotClass}`} />
            {f.label}
            <span className="chip-filter-count">{counts[f.status]}</span>
          </button>
        ))}

        <div className="library-toolbar-spacer" />

        <button
          type="button"
          className="btn btn-secondary"
          style={{ borderStyle: "dashed" }}
          onClick={() => setPreviewEmpty((v) => !v)}
          title="Preview empty state"
        >
          {previewEmpty ? <EyeSlash size={15} /> : <Eye size={15} />}
          {previewEmpty ? "Show populated library" : "Preview empty state"}
        </button>

        <div className="sort-dropdown">
          <button type="button" className="btn btn-secondary" onClick={() => setSortOpen((v) => !v)}>
            <SortAscending size={14} />
            {SORTS.find((s) => s.key === sort)?.label}
            <CaretDown size={11} />
          </button>
          {sortOpen && (
            <div className="sort-menu" onMouseLeave={() => setSortOpen(false)}>
              {SORTS.map((s) => (
                <button
                  key={s.key}
                  type="button"
                  className="sort-menu-item"
                  onClick={() => {
                    setSort(s.key);
                    setSortOpen(false);
                  }}
                >
                  <s.icon size={15} />
                  {s.label}
                  {sort === s.key && <Check size={13} weight="bold" className="check" />}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="seg">
          <button className={view === "grid" ? "active" : ""} onClick={() => setView("grid")} aria-label="Grid view">
            <SquaresFour size={14} />
            Grid
          </button>
          <button className={view === "list" ? "active" : ""} onClick={() => setView("list")} aria-label="List view">
            <ListBullets size={14} />
            List
          </button>
        </div>
      </div>

      {showEmpty ? (
        <EmptyLibrary />
      ) : view === "grid" ? (
        <div className="poster-grid">
          {filtered.map((item) => (
            <LibraryGridCard key={item.id} item={item} />
          ))}
        </div>
      ) : (
        <div className="library-list">
          <div className="library-list-header">
            <span>Title</span>
            <span>Year</span>
            <span>Quality</span>
            <span>Size</span>
            <span>Status</span>
            <span>Actions</span>
          </div>
          {filtered.map((item) => (
            <LibraryListRow key={item.id} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}
