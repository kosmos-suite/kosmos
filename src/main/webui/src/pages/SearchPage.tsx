import {
  BinocularsIcon as Binoculars,
  CompassIcon as Compass,
  DownloadSimpleIcon as DownloadSimple,
  LightningIcon as Lightning,
  MagnifyingGlassIcon as MagnifyingGlass,
  SlidersHorizontalIcon as SlidersHorizontal,
  SquaresFourIcon as SquaresFour,
  UsersThreeIcon as UsersThree,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import type { MetadataSearchResult } from "../api/types";
import { useAddToLibrary } from "../hooks/useAddToLibrary";
import { useApi } from "../hooks/useApi";
import { discoverItemLink } from "../utils/discoverItemLink";
import { tonalGradient } from "../utils/tonalGradient";
import { MediaCard } from "../components/MediaCard";

type Kind = "All" | "Movies" | "Series" | "Anime";

const RECENT_SEARCHES_KEY = "kosmos.recentSearches";
const RECENT_SEARCHES_LIMIT = 8;

function loadRecentSearches(): string[] {
  try {
    const raw = localStorage.getItem(RECENT_SEARCHES_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === "string") : [];
  } catch {
    return [];
  }
}

function persistRecentSearches(entries: string[]) {
  try {
    localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(entries));
  } catch {
    // best-effort — private browsing / storage full shouldn't break search
  }
}

function resultLink(result: MetadataSearchResult): string {
  if (!result.mediaItemId) return "#";
  if (result.mediaType === "tv") return `/shows/${result.mediaItemId}`;
  if (result.mediaType === "anime") return `/anime/${result.mediaItemId}`;
  return `/movies/${result.mediaItemId}`;
}

const RAIL_ITEMS = [
  { to: "/", icon: Compass },
  { to: "/library", icon: SquaresFour },
  { to: "/requests", icon: UsersThree },
  { to: "/activity", icon: DownloadSimple },
  { to: "/settings/indexers", icon: SlidersHorizontal },
];

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  const [kind, setKind] = useState<Kind>("All");
  const [results, setResults] = useState<MetadataSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const [recentSearches, setRecentSearches] = useState<string[]>(() => loadRecentSearches());
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const { admin, stateFor, triggerAdd } = useAddToLibrary();

  const { data: trending } = useApi(api.discoverTrending);
  const { stateFor: trendingStateFor, triggerAdd: triggerTrendingAdd } = useAddToLibrary();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // Live search, debounced — no submit button per the design ("no submit needed").
  useEffect(() => {
    const q = query.trim();
    if (!q) {
      setResults([]);
      setError(null);
      setLatencyMs(null);
      return;
    }
    const handle = setTimeout(async () => {
      setLoading(true);
      setError(null);
      const start = performance.now();
      try {
        const r = await api.searchMetadata(q);
        setResults(r);
        setLatencyMs(Math.round(performance.now() - start));
        setRecentSearches((current) => {
          const related = (existing: string) =>
            existing.toLowerCase().startsWith(q.toLowerCase()) || q.toLowerCase().startsWith(existing.toLowerCase());
          const next = [q, ...current.filter((existing) => !related(existing))].slice(0, RECENT_SEARCHES_LIMIT);
          persistRecentSearches(next);
          return next;
        });
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    }, 250);
    return () => clearTimeout(handle);
  }, [query]);

  const visibleResults =
    kind === "All"
      ? results
      : kind === "Movies"
        ? results.filter((r) => r.mediaType === "movie")
        : kind === "Series"
          ? results.filter((r) => r.mediaType === "tv")
          : results.filter((r) => r.mediaType === "anime");
  const hasQuery = query.trim() !== "";
  const hasResults = hasQuery && visibleResults.length > 0;
  const isNone = hasQuery && !loading && visibleResults.length === 0;
  const isIdle = !hasQuery;
  const newCount = visibleResults.filter((r) => !r.inLibrary).length;
  const inLibraryCount = visibleResults.length - newCount;

  useEffect(() => {
    setFocusedIndex(-1);
  }, [kind, results]);

  // Esc clears the query first, then (pressed again with an already-empty field) dismisses the
  // overlay back to wherever the user opened search from. Up/Down/Enter move through and act on
  // real results — this is a command-palette-style overlay, so arrows are captured globally rather
  // than moving the text cursor.
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        if (query.trim() !== "") {
          setQuery("");
        } else {
          navigate(-1);
        }
        return;
      }
      if (!hasResults) return;
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setFocusedIndex((i) => Math.min(i + 1, visibleResults.length - 1));
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setFocusedIndex((i) => Math.max(i - 1, 0));
      } else if (e.key === "Enter" && focusedIndex >= 0) {
        const result = visibleResults[focusedIndex];
        if (result.inLibrary) {
          navigate(resultLink(result));
        } else if (stateFor(result.externalId) === "idle") {
          triggerAdd({ ...result, externalId: result.externalId });
        }
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, navigate, hasResults, visibleResults, focusedIndex, stateFor, triggerAdd]);

  return (
    <div className="search-overlay">
      <div className="search-backdrop" />

      <button type="button" className="chip-floating search-close-btn" onClick={() => navigate(-1)} aria-label="Close search">
        <X size={14} />
        Close
      </button>

      <nav className="search-rail">
        {RAIL_ITEMS.map(({ to, icon: Icon }) => (
          <Link key={to} to={to} className="search-rail-item">
            <Icon size={19} />
          </Link>
        ))}
      </nav>

      <div className="search-container">
        <div className={`search-input-pill${hasQuery ? " filled" : ""}`}>
          <MagnifyingGlass size={22} className="search-input-icon" />
          <input
            ref={inputRef}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search for a movie, series, or anime…"
            aria-label="Search"
          />
          {hasQuery && (
            <span className="search-result-count">
              {loading ? "searching…" : `${visibleResults.length} result${visibleResults.length === 1 ? "" : "s"}`}
            </span>
          )}
          {hasQuery && (
            <button
              type="button"
              className="search-clear-btn"
              onClick={() => {
                setQuery("");
                inputRef.current?.focus();
              }}
              aria-label="Clear search"
            >
              <X size={14} />
            </button>
          )}
          <span className="search-esc-kbd">esc</span>
        </div>

        <div className="search-toolbar">
          <div className="filter-tabs" style={{ margin: 0 }}>
            {(["All", "Movies", "Series", "Anime"] as Kind[]).map((k) => (
              <button key={k} className={kind === k ? "active" : ""} onClick={() => setKind(k)}>
                {k}
              </button>
            ))}
          </div>
          <div className="search-toolbar-spacer" />
          <span className="search-latency">
            <Lightning size={13} />
            {hasQuery && latencyMs != null ? `searched TMDB in ${latencyMs} ms` : "live search — no submit needed"}
          </span>
        </div>

        {error && (
          <p className="text-muted" style={{ marginTop: 20 }}>
            {error}
          </p>
        )}

        {isIdle && !error && (
          <>
            {recentSearches.length > 0 && (
              <>
                <div className="search-section-label">Recent</div>
                <div className="search-recent-row">
                  {recentSearches.map((r) => (
                    <button key={r} type="button" className="search-recent-chip" onClick={() => setQuery(r)}>
                      {r}
                    </button>
                  ))}
                </div>
              </>
            )}

            <div className="search-eyebrow">Trending right now</div>
            <div className="search-sub">Movies &amp; series trending this week, from TMDB</div>
            <div className="poster-grid">
              {trending?.map((item, i) => (
                <MediaCard
                  key={item.mediaItemId ?? item.externalId}
                  to={discoverItemLink(item)}
                  title={item.title}
                  year={item.year}
                  posterPath={item.posterPath}
                  mediaItemId={item.mediaItemId}
                  mediaType={item.mediaType}
                  status={item.inLibrary ? (item.partiallyAvailable ? "partially-available" : "in-library") : undefined}
                  placeholderBackground={tonalGradient(i)}
                  onAdd={
                    item.inLibrary || !item.externalId
                      ? undefined
                      : () =>
                          triggerTrendingAdd({
                            externalId: item.externalId!,
                            title: item.title,
                            year: item.year,
                            overview: item.overview,
                            posterPath: item.posterPath,
                            backdropPath: item.backdropPath,
                            mediaType: item.mediaType,
                          })
                  }
                  addState={item.externalId ? trendingStateFor(item.externalId) : undefined}
                />
              ))}
            </div>
          </>
        )}

        {hasResults && (
          <>
            <div className="search-eyebrow">Results</div>
            <div className="search-sub">
              {newCount} new{inLibraryCount > 0 ? ` · ${inLibraryCount} already in your library` : ""}
            </div>
            <div className="poster-grid">
              {visibleResults.map((result, i) => (
                <MediaCard
                  key={result.externalId}
                  className={i === focusedIndex ? "focused" : undefined}
                  to={resultLink(result)}
                  title={result.title}
                  year={result.year}
                  posterPath={result.posterPath}
                  mediaItemId={result.mediaItemId}
                  mediaType={result.mediaType}
                  status={result.inLibrary ? (result.partiallyAvailable ? "partially-available" : "in-library") : undefined}
                  placeholderBackground={tonalGradient(i)}
                  onAdd={result.inLibrary ? undefined : () => triggerAdd(result)}
                  addState={stateFor(result.externalId)}
                />
              ))}
            </div>
          </>
        )}

        {isNone && !error && (
          <div className="search-none-state">
            <div className="search-none-icon">
              <Binoculars size={28} />
            </div>
            <div className="empty-state-title">Nothing matched "{query}"</div>
            <p className="empty-state-body">
              For anime, try the romanized Japanese title if the English name doesn't turn anything up — TMDB
              indexes most series that way.
            </p>
            <div className="search-none-actions">
              <button type="button" className="search-action-chip" onClick={() => setQuery("")}>
                Clear and browse trending
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="search-hints-bar">
        <span className="search-hint">
          <kbd>↑↓</kbd> navigate
        </span>
        <span className="search-hint">
          <kbd>↩</kbd> {admin ? "add to library" : "request"}
        </span>
        <div className="search-hints-spacer" />
        <span className="search-source-line">{isIdle ? "TMDB trending" : "TMDB · AniList"}</span>
      </div>
    </div>
  );
}
