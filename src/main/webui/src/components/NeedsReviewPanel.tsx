import {
  ArrowSquareOutIcon as ArrowSquareOut,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  FilmStripIcon as FilmStrip,
  MagnifyingGlassIcon as MagnifyingGlass,
  PlanetIcon as Planet,
  SpinnerIcon as Spinner,
  StarIcon as Star,
  XIcon as X,
} from "@phosphor-icons/react";
import { useEffect, useState, type MouseEvent as ReactMouseEvent } from "react";
import { api } from "../api/client";
import type { MetadataSearchResult, UnclassifiedShow } from "../api/types";
import { posterUrl } from "../api/tmdbImage";

/**
 * Jellyfin reports both "tvshows" and anime under the same item type — most of the time Kosmos can
 * tell them apart on its own (Jellyfin's own AniList id, or a Fribb/TMDB cross-reference), but
 * sometimes that signal points at anime and AniList itself has nothing to confirm it with. Rather
 * than guessing, those land here for a person to resolve — inspired by Jellyfin's own "Identify"
 * flow and Sonarr/Radarr's manual search: the safe default (keep it a Series, since the TMDB
 * identity is already trusted) is one click away, and searching AniList for the right match is the
 * fallback when the title really is anime.
 */
export function NeedsReviewPanel() {
  const [items, setItems] = useState<UnclassifiedShow[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .listUnclassifiedShows()
      .then((r) => !cancelled && setItems(r))
      .catch((e: unknown) => !cancelled && setError(e instanceof Error ? e.message : String(e)))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, []);

  function removeItem(id: string) {
    setItems((prev) => (prev ? prev.filter((i) => i.id !== id) : prev));
  }

  if (loading) {
    return null;
  }

  if (error) {
    return <p className="text-muted">Failed to load: {error}</p>;
  }

  if (!items || items.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-state-inner">
          <div
            style={{
              width: 62,
              aspectRatio: "2 / 3",
              margin: "0 auto 24px",
              borderRadius: 11,
              background: "var(--bg)",
              border: "1px solid rgba(145,132,217,.28)",
              display: "grid",
              placeItems: "center",
              boxShadow: "0 16px 36px rgba(0,0,0,.5)",
            }}
          >
            <Planet size={22} color="var(--accent-tint)" />
          </div>
          <div className="empty-state-title">Nothing needs review</div>
          <p className="empty-state-body">
            Every Jellyfin title Kosmos couldn't confidently sort into Series or Anime shows up here after a sync.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="review-list">
      {items.map((item) => (
        <ReviewCard key={item.id} item={item} onResolved={() => removeItem(item.id)} />
      ))}
    </div>
  );
}

interface HoverInfo {
  result: MetadataSearchResult;
  top: number;
  left: number;
  flip: boolean;
}

function ReviewCard({ item, onResolved }: { item: UnclassifiedShow; onResolved: () => void }) {
  const [busy, setBusy] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<MetadataSearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [hovered, setHovered] = useState<HoverInfo | null>(null);

  useEffect(() => {
    if (!searchOpen) return;
    const q = query.trim();
    if (!q) {
      setResults([]);
      return;
    }
    const handle = setTimeout(async () => {
      setSearching(true);
      try {
        const r = await api.searchMetadata(q);
        const anime = r.filter((m) => m.mediaType === "anime");
        anime.sort((a, b) => (a.year ?? 9999) - (b.year ?? 9999));
        setResults(anime);
      } catch {
        setResults([]);
      } finally {
        setSearching(false);
      }
    }, 250);
    return () => clearTimeout(handle);
  }, [query, searchOpen]);

  async function confirmAsShow() {
    setBusy(true);
    setActionError(null);
    try {
      await api.resolveUnclassifiedAsShow(item.id);
      onResolved();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : String(e));
      setBusy(false);
    }
  }

  async function confirmAsAnime(anilistId: string) {
    setBusy(true);
    setActionError(null);
    try {
      await api.resolveUnclassifiedAsAnime(item.id, anilistId);
      onResolved();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : String(e));
      setBusy(false);
    }
  }

  function handleResultEnter(e: ReactMouseEvent<HTMLDivElement>, r: MetadataSearchResult) {
    const rect = e.currentTarget.getBoundingClientRect();
    const flip = rect.right + 380 > window.innerWidth;
    setHovered({ result: r, top: rect.top, left: flip ? rect.left - 12 : rect.right + 12, flip });
  }

  function handleResultLeave() {
    setHovered(null);
  }

  async function dismiss() {
    setBusy(true);
    setActionError(null);
    try {
      await api.dismissUnclassified(item.id);
      onResolved();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : String(e));
      setBusy(false);
    }
  }

  const src = posterUrl(item.posterPath);

  return (
    <div className="review-card">
      <div className="review-card-art">
        {src ? (
          <img className="review-card-poster" src={src} alt="" loading="lazy" />
        ) : (
          <div className="review-card-placeholder">
            <FilmStrip size={22} />
          </div>
        )}
      </div>

      <div className="review-card-body">
        <div className="review-card-title-row">
          <span className="review-card-title">{item.name}</span>
          {item.year && <span className="review-card-year">{item.year}</span>}
        </div>
        {item.seasons.length > 0 && (
          <div className="review-card-seasons">
            {item.seasons.map((s) => (
              <span key={s.seasonNumber} className="review-card-season-chip">
                {s.seasonNumber === 0 ? "Specials" : `S${s.seasonNumber}`}
                <span className="review-card-season-chip-count">{s.episodeCount}</span>
              </span>
            ))}
          </div>
        )}
        <span className="review-card-badge">
          <span className="review-card-badge-dot" />
          Needs review
        </span>
        <p className="review-card-hint">
          {item.reason === "ANILIST_MATCH_AMBIGUOUS"
            ? "Jellyfin reports a possible AniList match, but its title and release year don't both line up — could be right, could be a bad match."
            : "Might be anime, but AniList couldn't confirm it."}
          {item.anilistId ? ` (Jellyfin reports AniList #${item.anilistId})` : ""}
        </p>
        {item.overview && <p className="review-card-overview">{item.overview}</p>}
        {actionError && <p className="review-card-error">{actionError}</p>}

        <div className="review-card-actions">
          <button type="button" className="btn btn-primary" disabled={busy} onClick={confirmAsShow}>
            {busy ? <Spinner size={14} className="spin" /> : <Check size={14} weight="bold" />}
            Confirm as Series
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            disabled={busy}
            onClick={() =>
              setSearchOpen((v) => {
                const next = !v;
                if (next) setQuery(item.name);
                return next;
              })
            }
          >
            <MagnifyingGlass size={14} />
            Find on AniList
            <CaretDown size={11} style={{ transform: searchOpen ? "rotate(180deg)" : undefined }} />
          </button>
          <button type="button" className="btn btn-ghost btn-icon" disabled={busy} onClick={dismiss} title="Dismiss">
            <X size={14} />
          </button>
        </div>

        {searchOpen && (
          <div className="review-card-search">
            <input
              className="input"
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search AniList for the correct match…"
            />
            {searching && <p className="text-faint review-card-search-status">Searching…</p>}
            {!searching && query.trim() && results.length === 0 && (
              <p className="text-faint review-card-search-status">No AniList matches.</p>
            )}
            {results.length > 0 && (
              <div className="review-card-results">
                {results.map((r) => (
                  <div
                    key={r.externalId}
                    className="review-card-result"
                    onMouseEnter={(e) => handleResultEnter(e, r)}
                    onMouseLeave={handleResultLeave}
                  >
                    <button
                      type="button"
                      className="review-card-result-main"
                      disabled={busy}
                      onClick={() => confirmAsAnime(r.externalId)}
                    >
                      {r.posterPath ? (
                        <img className="review-card-result-poster" src={posterUrl(r.posterPath) ?? undefined} alt="" loading="lazy" />
                      ) : (
                        <div className="review-card-result-poster review-card-placeholder">
                          <FilmStrip size={14} />
                        </div>
                      )}
                      <span className="review-card-result-title">{r.title}</span>
                      <span className="review-card-result-year">{r.year ?? "—"}</span>
                      {r.episodeCount != null && (
                        <span className="review-card-result-episodes">{r.episodeCount} ep</span>
                      )}
                    </button>
                    <a
                      className="review-card-result-external"
                      href={`https://anilist.co/anime/${r.externalId}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      title="Open on AniList"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <ArrowSquareOut size={13} />
                    </a>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {hovered && (
        <div
          className="review-tooltip"
          style={{
            top: hovered.top,
            left: hovered.left,
            transform: hovered.flip ? "translateX(-100%)" : undefined,
          }}
        >
          {hovered.result.posterPath ? (
            <img className="review-tooltip-poster" src={posterUrl(hovered.result.posterPath) ?? undefined} alt="" />
          ) : (
            <div className="review-tooltip-poster review-card-placeholder">
              <FilmStrip size={20} />
            </div>
          )}
          <div className="review-tooltip-body">
            <div className="review-tooltip-title">{hovered.result.title}</div>
            <div className="review-tooltip-meta">
              {hovered.result.year ?? "Unknown year"}
              {hovered.result.episodeCount != null && <span>{hovered.result.episodeCount} episodes</span>}
              {hovered.result.voteAverage != null && (
                <span className="review-tooltip-score">
                  <Star size={11} weight="fill" />
                  {hovered.result.voteAverage.toFixed(1)}
                </span>
              )}
            </div>
            {hovered.result.overview && <p className="review-tooltip-overview">{hovered.result.overview}</p>}
          </div>
        </div>
      )}
    </div>
  );
}
