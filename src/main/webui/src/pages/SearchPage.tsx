import {
  BinocularsIcon as Binoculars,
  CompassIcon as Compass,
  DownloadSimpleIcon as DownloadSimple,
  FilmStripIcon as FilmStrip,
  LightningIcon as Lightning,
  MagnifyingGlassIcon as MagnifyingGlass,
  PlusIcon as Plus,
  SlidersHorizontalIcon as SlidersHorizontal,
  SpinnerIcon as Spinner,
  SquaresFourIcon as SquaresFour,
  UsersThreeIcon as UsersThree,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import type { MetadataSearchResult } from "../api/types";
import { posterUrl } from "../api/tmdbImage";
import { useAuth } from "../auth/AuthContext";
import { recentSearches, trendingSearches } from "../mocks/mockSearch";
import { tonalGradient } from "../utils/tonalGradient";

type Kind = "All" | "Movies" | "Series" | "Anime";

function kindLabel(mediaType: MetadataSearchResult["mediaType"]): string {
  if (mediaType === "tv") return "Series";
  if (mediaType === "anime") return "Anime";
  return "Movie";
}

// Anime results come from AniList; movie/tv results come from TMDB — see MetadataResource.search().
function sourceLabel(mediaType: MetadataSearchResult["mediaType"]): string {
  return mediaType === "anime" ? "AniList" : "TMDB";
}

const RAIL_ITEMS = [
  { to: "/", icon: Compass },
  { to: "/library", icon: SquaresFour },
  { to: "/requests", icon: UsersThree },
  { to: "/activity", icon: DownloadSimple },
  { to: "/settings/indexers", icon: SlidersHorizontal },
];

export default function SearchPage() {
  const { user } = useAuth();
  const admin = user?.role === "ADMIN";
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  const [kind, setKind] = useState<Kind>("All");
  const [results, setResults] = useState<MetadataSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [addingId, setAddingId] = useState<string | null>(null);
  const [requestedIds, setRequestedIds] = useState<Set<string>>(new Set());
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

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
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    }, 250);
    return () => clearTimeout(handle);
  }, [query]);

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setQuery("");
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  async function addResult(result: MetadataSearchResult) {
    setAddingId(result.externalId);
    const pluginSlug = result.mediaType === "anime" ? "anilist" : "tmdb";
    try {
      if (!admin) {
        await api.createRequest({
          externalId: result.externalId,
          pluginSlug,
          mediaType: result.mediaType,
          title: result.title,
          year: result.year,
          overview: result.overview,
          posterPath: result.posterPath,
          backdropPath: result.backdropPath,
        });
        setRequestedIds((s) => new Set(s).add(result.externalId));
        setAddingId(null);
        return;
      }
      if (result.mediaType === "tv") {
        const show = await api.createShow({
          externalId: result.externalId,
          pluginSlug,
          title: result.title,
          year: result.year,
          overview: result.overview,
          posterPath: result.posterPath,
          backdropPath: result.backdropPath,
        });
        navigate(`/shows/${show.id}`);
      } else if (result.mediaType === "anime") {
        const anime = await api.createAnime({
          externalId: result.externalId,
          pluginSlug,
          title: result.title,
          year: result.year,
          overview: result.overview,
          posterPath: result.posterPath,
          backdropPath: result.backdropPath,
        });
        navigate(`/anime/${anime.id}`);
      } else {
        const movie = await api.createMovie({
          externalId: result.externalId,
          pluginSlug,
          title: result.title,
          year: result.year,
          overview: result.overview,
          posterPath: result.posterPath,
          backdropPath: result.backdropPath,
        });
        navigate(`/movies/${movie.id}`);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setAddingId(null);
    }
  }

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

  return (
    <div className="search-overlay">
      <div className="search-backdrop" />

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
            <div className="search-section-label">Recent</div>
            <div className="search-recent-row">
              {recentSearches.map((r) => (
                <button key={r} type="button" className="search-recent-chip" onClick={() => setQuery(r)}>
                  {r}
                </button>
              ))}
            </div>

            <div className="search-eyebrow">Trending right now</div>
            <div className="search-sub">Popular this week across TMDB and your indexers</div>
            <div className="search-result-grid">
              {trendingSearches.map((t) => (
                <div key={t.id} className="similar-card">
                  <div className="similar-card-art">
                    <div className="movie-card-placeholder" style={{ background: tonalGradient(t.tone) }}>
                      <FilmStrip size={28} />
                    </div>
                    <div className="rank-badge">#{t.rank}</div>
                    <div className="similar-card-scrim" />
                    <div className="similar-card-action">
                      <button type="button" style={{ background: "var(--accent-gradient)", color: "#0B0C12" }}>
                        <Plus size={13} />
                        Add
                      </button>
                    </div>
                  </div>
                  <div className="similar-card-title">{t.title}</div>
                  <div className="similar-card-meta">
                    {t.year} · {t.kind}
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {hasResults && (
          <>
            <div className="search-eyebrow">New results</div>
            <div className="search-sub">{visibleResults.length} not yet in your library</div>
            <div className="search-result-grid">
              {visibleResults.map((result) => {
                const src = posterUrl(result.posterPath);
                const adding = addingId === result.externalId;
                const requested = requestedIds.has(result.externalId);
                return (
                  <div key={result.externalId} className="similar-card">
                    <div className="similar-card-art">
                      {src ? (
                        <img className="movie-card-poster" src={src} alt="" loading="lazy" />
                      ) : (
                        <div className="movie-card-placeholder">
                          <MagnifyingGlass size={28} />
                        </div>
                      )}
                      <div
                        className="similar-card-badge"
                        style={{
                          background: "rgba(145,132,217,.2)",
                          border: "1px solid rgba(145,132,217,.42)",
                          color: "#D2CEFD",
                        }}
                      >
                        {sourceLabel(result.mediaType)}
                      </div>
                      <div className="search-kind-tag">{kindLabel(result.mediaType)}</div>
                      <div className="similar-card-scrim" />
                      <div className="similar-card-action">
                        <button
                          type="button"
                          style={
                            requested
                              ? { background: "rgba(233,233,237,.16)", color: "#E9E9ED" }
                              : { background: "var(--accent-gradient)", color: "#0B0C12" }
                          }
                          disabled={adding || requested}
                          onClick={() => addResult(result)}
                        >
                          {adding ? <Spinner size={13} className="spin" /> : <Plus size={13} />}
                          {adding ? (admin ? "Adding…" : "Requesting…") : requested ? "Requested" : admin ? "Add" : "Request"}
                        </button>
                      </div>
                    </div>
                    <div className="similar-card-title">{result.title}</div>
                    <div className="similar-card-meta">
                      {result.year ? `${result.year} · ` : ""}
                      {kindLabel(result.mediaType)}
                    </div>
                  </div>
                );
              })}
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
              <button type="button" className="search-action-chip" disabled title="Not wired up yet">
                Search by TMDB or IMDb ID
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
          <kbd>↩</kbd> add to library
        </span>
        <span className="search-hint">
          <kbd>⌘↩</kbd> open details
        </span>
        <div className="search-hints-spacer" />
        <span className="search-source-line">{isIdle ? "TMDB trending · refreshed 2h ago" : "TMDB · metadata proxy"}</span>
      </div>
    </div>
  );
}
