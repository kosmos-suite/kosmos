import {
  ArrowsClockwiseIcon as ArrowsClockwise,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  CellSignalFullIcon as CellSignalFull,
  CellSignalLowIcon as CellSignalLow,
  CellSignalMediumIcon as CellSignalMedium,
  CellSignalSlashIcon as CellSignalSlash,
  CaretUpIcon as CaretUp,
  CircleNotchIcon as CircleNotch,
  DownloadSimpleIcon as DownloadSimple,
  ListMagnifyingGlassIcon as ListMagnifyingGlass,
  ProhibitIcon as Prohibit,
  SortDescendingIcon as SortDescending,
  WarningIcon as Warning,
  XIcon as X,
  XCircleIcon as XCircle,
} from "@phosphor-icons/react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { posterUrl } from "../api/tmdbImage";
import { useApi } from "../hooks/useApi";
import type { CustomFormatMatch, DownloadClient, Indexer, QualityProfile, ScoredSearchResult } from "../api/types";

type SortKey = "Score" | "Size" | "Seeders" | "Date";
type ViewKey = "Show all results" | "Show only qualifying";
type GrabState = "idle" | "sending" | "grabbed";

interface DisplayRelease {
  key: string;
  title: string;
  score: number;
  cutoffScore: number;
  rejectionReason: string | null;
  pills: string[];
  indexerName: string;
  ageLabel: string;
  publishedAt: string | null;
  sizeGb: number;
  seeders: number;
  peers: number;
  downloadUrl: string;
  resolution: string | null;
  source: string | null;
  videoCodec: string | null;
  breakdown: CustomFormatMatch[];
}

function toDisplay(result: ScoredSearchResult, indexerName: string): DisplayRelease {
  const score = result.score ?? 0;
  const cutoff = result.cutoffScore ?? 0;
  return {
    key: `${indexerName}-${result.raw.title}`,
    title: result.raw.title,
    score,
    cutoffScore: cutoff,
    rejectionReason: result.sizeGateReason ?? (result.passesCutoff === false ? `Scored ${score} of ${cutoff} needed` : null),
    pills: [result.parsed.resolution, result.parsed.source, result.parsed.videoCodec].filter((v): v is string => !!v),
    indexerName,
    ageLabel: result.raw.publishedAt ? new Date(result.raw.publishedAt).toLocaleDateString() : "—",
    publishedAt: result.raw.publishedAt,
    sizeGb: result.raw.sizeBytes / 1024 ** 3,
    seeders: result.raw.seeders ?? 0,
    peers: result.raw.peers ?? 0,
    downloadUrl: result.raw.downloadUrl,
    resolution: result.parsed.resolution,
    source: result.parsed.source,
    videoCodec: result.parsed.videoCodec,
    breakdown: result.formatBreakdown ?? [],
  };
}

function ageRank(iso: string | null): number {
  return iso ? new Date(iso).getTime() : 0;
}

function signal(seeders: number) {
  if (seeders === 0) return { Icon: CellSignalSlash, color: "var(--status-bad)" };
  if (seeders < 10) return { Icon: CellSignalLow, color: "var(--status-warn)" };
  if (seeders < 80) return { Icon: CellSignalMedium, color: "var(--text-secondary)" };
  return { Icon: CellSignalFull, color: "var(--status-good)" };
}

function scoreTone(passes: boolean, dim: boolean): { bg: string; fg: string } {
  if (dim) return { bg: "rgba(233,233,237,.05)", fg: "#75798C" };
  if (passes) return { bg: "rgba(79,191,139,.16)", fg: "var(--status-good-text)" };
  return { bg: "rgba(224,169,74,.16)", fg: "var(--status-warn-text)" };
}

interface ReleaseRowProps {
  release: DisplayRelease;
  dim: boolean;
  grabState: GrabState;
  onGrab: () => void;
}

function ReleaseRow({ release: r, dim, grabState, onGrab }: ReleaseRowProps) {
  const [whyOpen, setWhyOpen] = useState(false);
  const { Icon: SignalIcon, color: signalColor } = signal(r.seeders);
  const tone = scoreTone(!r.rejectionReason, dim);
  const btnLabel = grabState === "grabbed" ? "Grabbed" : grabState === "sending" ? "Sending" : dim ? "Grab anyway" : "Grab";
  const btnDisabled = grabState !== "idle";
  const hasBreakdown = r.breakdown.length > 0;
  const matched = r.breakdown.filter((m) => m.matched);

  return (
    <div className={`release-row2${dim ? " dim" : ""}${grabState === "grabbed" ? " grabbed" : ""}`}>
      <div className="release-score2">
        <span
          className="box"
          style={{ background: tone.bg, color: tone.fg, cursor: hasBreakdown ? "pointer" : undefined }}
          onClick={hasBreakdown ? () => setWhyOpen((o) => !o) : undefined}
          title={hasBreakdown ? "Why did this score this way?" : undefined}
        >
          {r.score}
        </span>
        <span className="lbl">score</span>
        {hasBreakdown && (
          <span
            className="text-faint"
            style={{ fontSize: 10, cursor: "pointer", display: "flex", alignItems: "center", gap: 1 }}
            onClick={() => setWhyOpen((o) => !o)}
          >
            why{whyOpen ? <CaretUp size={9} /> : <CaretDown size={9} />}
          </span>
        )}
      </div>

      <div className="release-main2">
        <div className="release-title2" style={dim ? { color: "var(--text-muted)" } : undefined}>
          {r.title}
        </div>
        <div className="release-pills2">
          {r.rejectionReason && (
            <span
              className="release-reason-tag"
              style={{
                background: "rgba(224,169,74,.1)",
                border: "1px solid rgba(224,169,74,.22)",
                color: "var(--status-warn-text)",
              }}
            >
              <Warning size={11} />
              {r.rejectionReason}
            </span>
          )}
          {r.pills.map((p) => (
            <span key={p} className="release-pill2">
              {p}
            </span>
          ))}
          <span className="release-source2">
            {r.indexerName} · {r.ageLabel}
          </span>
        </div>
        {whyOpen && hasBreakdown && (
          <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 8 }}>
            {matched.length === 0 ? (
              <span className="text-faint" style={{ fontSize: 11 }}>
                None of your custom formats matched this release.
              </span>
            ) : (
              matched.map((m) => (
                <span
                  key={m.customFormatId}
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 4,
                    fontSize: 11,
                    padding: "2px 7px",
                    borderRadius: 5,
                    background: m.score >= 0 ? "rgba(79,191,139,.12)" : "rgba(224,104,95,.12)",
                    border: `1px solid ${m.score >= 0 ? "rgba(79,191,139,.28)" : "rgba(224,104,95,.28)"}`,
                    color: m.score >= 0 ? "var(--status-good-text)" : "var(--status-bad-text)",
                  }}
                >
                  {m.score >= 0 ? <Check size={10} /> : <XCircle size={10} />}
                  {m.name} {m.score >= 0 ? "+" : ""}
                  {m.score}
                </span>
              ))
            )}
          </div>
        )}
      </div>

      <div className="release-size2">
        <div className="size">{r.sizeGb.toFixed(1)} GB</div>
        <div className="protocol">Torrent</div>
      </div>

      <div className="release-signal">
        <SignalIcon size={15} color={signalColor} />
        <div style={{ textAlign: "right" }}>
          <div className="num" style={{ color: signalColor }}>
            {r.seeders === 0 ? "none" : r.seeders}
          </div>
          <div className="peers">{r.seeders === 0 ? "stalled" : `${r.peers} peers`}</div>
        </div>
      </div>

      <button
        type="button"
        className="release-grab-btn"
        disabled={btnDisabled}
        onClick={onGrab}
        style={{
          background: grabState === "grabbed" ? "rgba(79,191,139,.16)" : grabState === "sending" ? "rgba(145,132,217,.2)" : dim ? "transparent" : "var(--accent-gradient)",
          border: grabState === "grabbed" ? "1px solid rgba(79,191,139,.3)" : dim && grabState === "idle" ? "1px solid var(--border-strong)" : "0",
          color: grabState === "grabbed" ? "var(--status-good-text)" : grabState === "sending" ? "#D2CEFD" : dim ? "var(--text-secondary)" : "#0B0C12",
          cursor: btnDisabled ? "default" : "pointer",
        }}
      >
        {grabState === "grabbed" ? (
          <Check size={13} />
        ) : grabState === "sending" ? (
          <CircleNotch size={13} className="spin" />
        ) : (
          <DownloadSimple size={13} />
        )}
        {btnLabel}
      </button>
    </div>
  );
}

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

export default function InteractiveSearchPage() {
  const { id } = useParams<{ id: string }>();
  const pathname = useLocation().pathname;
  const isEpisode = pathname.startsWith("/episodes/");
  const isAnimeEpisode = pathname.startsWith("/anime-episodes/");
  const navigate = useNavigate();
  const { data: movie } = useApi(
    () => (isEpisode || isAnimeEpisode ? Promise.resolve(null) : api.getMovie(id!)),
    [id, isEpisode, isAnimeEpisode],
  );
  const { data: episode } = useApi(() => (isEpisode ? api.getEpisode(id!) : Promise.resolve(null)), [id, isEpisode]);
  const { data: animeEpisode } = useApi(
    () => (isAnimeEpisode ? api.getAnimeEpisode(id!) : Promise.resolve(null)),
    [id, isAnimeEpisode],
  );
  const { data: indexers } = useApi(() => api.listIndexers(), []);
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const { data: downloadClients } = useApi(() => api.listDownloadClients(), []);

  const animeEpisodeNumber = animeEpisode ? (animeEpisode.absoluteEpisodeNumber ?? animeEpisode.episodeNumber) : null;
  const searchQuery = movie
    ? movie.title
    : episode
      ? `${episode.showTitle} S${pad(episode.seasonNumber)}E${pad(episode.episodeNumber)}`
      : animeEpisode && animeEpisodeNumber != null
        ? `${animeEpisode.animeTitle} - ${pad(animeEpisodeNumber)}`
        : null;
  const runtimeMinutes = movie ? movie.runtimeMinutes : (episode?.runtimeMinutes ?? animeEpisode?.runtimeMinutes ?? null);
  const profile: QualityProfile | undefined = profiles?.[0];
  const downloadClient: DownloadClient | undefined = downloadClients?.[0];
  const enabledIndexers: Indexer[] = useMemo(() => (indexers ?? []).filter((i) => i.enabled), [indexers]);

  const [results, setResults] = useState<DisplayRelease[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [elapsedMs, setElapsedMs] = useState<number | null>(null);

  const [view, setView] = useState<ViewKey>("Show all results");
  const [sort, setSort] = useState<SortKey>("Score");
  const [sortOpen, setSortOpen] = useState(false);
  const [showFails, setShowFails] = useState(true);
  const [grabbing, setGrabbing] = useState<Set<string>>(new Set());
  const [grabbed, setGrabbed] = useState<Set<string>>(new Set());
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<number | undefined>(undefined);

  const say = useCallback((msg: string) => {
    window.clearTimeout(toastTimer.current);
    setToast(msg);
    toastTimer.current = window.setTimeout(() => setToast(null), 3800);
  }, []);

  const runSearch = useCallback(async () => {
    if (!searchQuery || enabledIndexers.length === 0) return;
    setSearching(true);
    setSearchError(null);
    const startedAt = performance.now();
    try {
      const perIndexer = await Promise.all(
        enabledIndexers.map((idx) =>
          (profile
            ? api.searchIndexerScored(idx.id, searchQuery, profile.id, runtimeMinutes)
            : api.searchIndexer(idx.id, searchQuery, undefined, runtimeMinutes)
          ).then((rows) => rows.map((r) => toDisplay(r, idx.name))),
        ),
      );
      setResults(perIndexer.flat());
      setElapsedMs(performance.now() - startedAt);
    } catch (e) {
      setSearchError(e instanceof ApiError ? e.message : "Search failed");
    } finally {
      setSearching(false);
    }
  }, [searchQuery, runtimeMinutes, enabledIndexers, profile]);

  useEffect(() => {
    runSearch();
  }, [runSearch]);

  const sorter = useCallback(
    (a: DisplayRelease, b: DisplayRelease) => {
      if (sort === "Score") return b.score - a.score;
      if (sort === "Size") return b.sizeGb - a.sizeGb;
      if (sort === "Seeders") return b.seeders - a.seeders;
      return ageRank(b.publishedAt) - ageRank(a.publishedAt);
    },
    [sort],
  );

  const { passRows, failRows } = useMemo(() => {
    const pass = results.filter((r) => !r.rejectionReason).sort(sorter);
    const fail = results.filter((r) => r.rejectionReason).sort(sorter);
    return { passRows: pass, failRows: fail };
  }, [results, sorter]);

  const onlyQualifying = view === "Show only qualifying";
  const failsVisible = !onlyQualifying && showFails;

  const grab = async (r: DisplayRelease) => {
    if (grabbed.has(r.key) || grabbing.has(r.key)) return;
    if (!downloadClient) {
      say("Configure a download client under Settings first");
      return;
    }
    setGrabbing((s) => new Set(s).add(r.key));
    try {
      const grabFn = isEpisode || isAnimeEpisode ? api.grabEpisodeRelease : api.grabRelease;
      await grabFn(id!, {
        title: r.title,
        downloadUrl: r.downloadUrl,
        resolution: r.resolution,
        source: r.source,
        videoCodec: r.videoCodec,
        score: r.score,
        downloadClientId: downloadClient.id,
      });
      setGrabbed((s) => new Set(s).add(r.key));
      say(`Sent to ${downloadClient.name} · ${r.sizeGb.toFixed(1)} GB`);
    } catch (e) {
      say(e instanceof ApiError ? `Grab failed: ${e.message}` : "Grab failed");
    } finally {
      setGrabbing((s) => {
        const next = new Set(s);
        next.delete(r.key);
        return next;
      });
    }
  };

  const grabStateFor = (key: string): GrabState => {
    if (grabbed.has(key)) return "grabbed";
    if (grabbing.has(key)) return "sending";
    return "idle";
  };

  const resourceSegment = isAnimeEpisode ? "anime-episodes" : isEpisode ? "episodes" : "movies";

  const close = () =>
    navigate(
      movie
        ? `/movies/${id}`
        : episode
          ? `/shows/${episode.showId}`
          : animeEpisode
            ? `/anime/${animeEpisode.animeId}`
            : "/",
    );
  const posterSrc = movie ? posterUrl(movie.posterPath, "w185") : null;
  const chipTitle = movie ? movie.title : episode ? episode.title : (animeEpisode?.title ?? "…");
  const chipSub = movie
    ? String(movie.year ?? "")
    : episode
      ? `S${pad(episode.seasonNumber)}E${pad(episode.episodeNumber)}`
      : animeEpisodeNumber != null
        ? `EP${pad(animeEpisodeNumber)}`
        : "";

  return (
    <div className="modal-scrim">
      <div className="modal-scrim-art" />
      <div className="modal-scrim-dim" />

      <div className="modal-scrim-center">
        <div className="search-panel">
          <div className="search-panel-header">
            <div className="search-panel-title-row">
              <span className="search-panel-icon">
                <ListMagnifyingGlass size={19} />
              </span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 5, flexWrap: "wrap" }}>
                  <span style={{ fontSize: 17, fontWeight: 600, letterSpacing: "-0.02em" }}>Interactive Search</span>
                  <span className="tag tag-accent" style={{ textTransform: "uppercase", letterSpacing: "0.1em" }}>
                    you pick the release
                  </span>
                </div>
                <div className="search-panel-desc">
                  Real results from every enabled indexer, scored by your quality profile.
                </div>
              </div>
              <button type="button" className="btn btn-secondary" onClick={runSearch} disabled={searching}>
                {searching ? <CircleNotch size={14} className="spin" /> : <ArrowsClockwise size={14} />}
                {searching ? "Searching…" : "Search again"}
              </button>
              <button type="button" className="modal-close" onClick={close}>
                <X size={16} />
              </button>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: 14, flexWrap: "wrap" }}>
              <div className="search-panel-movie-chip">
                <div className="search-panel-movie-chip-art">{posterSrc && <img src={posterSrc} alt="" />}</div>
                <span style={{ fontWeight: 500, fontSize: 12.5 }}>{chipTitle}</span>
                <span className="text-ghost" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                  {chipSub}
                </span>
              </div>
              <span className="text-ghost" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                {profile ? `profile ${profile.name} · min score ${profile.cutoffScore}` : "no quality profile configured — results unscored"}
              </span>
              <div style={{ flex: 1 }} />
              <span className="text-faint" style={{ fontSize: 11.5 }}>
                {results.length} releases · {enabledIndexers.length} indexer{enabledIndexers.length === 1 ? "" : "s"}
                {elapsedMs != null ? ` · ${(elapsedMs / 1000).toFixed(1)}s` : ""}
              </span>
            </div>
          </div>

          <div className="search-panel-controls">
            <div className="seg">
              {(["Show all results", "Show only qualifying"] as ViewKey[]).map((label) => (
                <button
                  key={label}
                  type="button"
                  className={view === label ? "active" : ""}
                  onClick={() => {
                    setView(label);
                    setShowFails(label === "Show all results");
                  }}
                >
                  {label}
                </button>
              ))}
            </div>

            <div className="dropdown-wrap">
              <span className={`dropdown-trigger${sortOpen ? " open" : ""}`} onClick={() => setSortOpen((o) => !o)}>
                <SortDescending size={14} className="text-muted" />
                {sort}
                <CaretDown size={11} className="text-faint" />
              </span>
              {sortOpen && (
                <div className="dropdown-menu">
                  {(["Score", "Size", "Seeders", "Date"] as SortKey[]).map((label) => (
                    <div
                      key={label}
                      className={`dropdown-item${sort === label ? " active" : ""}`}
                      onClick={() => {
                        setSort(label);
                        setSortOpen(false);
                      }}
                    >
                      <span style={{ flex: 1 }}>{label}</span>
                      {sort === label && <Check size={13} color="var(--accent-tint)" />}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div style={{ flex: 1 }} />
            <span className="count-indicator" style={{ color: "var(--status-good-text)" }}>
              <span className="dot" style={{ background: "var(--status-good)" }} />
              {passRows.length} qualifying
            </span>
            <span className="count-indicator text-muted">
              <span className="dot" style={{ background: "var(--text-ghost)" }} />
              {failRows.length} below threshold
            </span>
          </div>

          <div className="search-panel-body k-scroll">
            {searchError && <p className="text-muted" style={{ padding: 16 }}>{searchError}</p>}
            {!searching && !searchError && enabledIndexers.length === 0 && (
              <p className="text-muted" style={{ padding: 16 }}>
                No enabled indexers — add one under Settings → Indexers.
              </p>
            )}

            {passRows.map((r) => (
              <ReleaseRow key={r.key} release={r} dim={false} grabState={grabStateFor(r.key)} onGrab={() => grab(r)} />
            ))}

            {failRows.length > 0 && (
              <>
                <div className="threshold-divider">
                  <div className="line" />
                  <span className="threshold-divider-btn" onClick={() => setShowFails((s) => !s)}>
                    <span className="lbl">Below threshold</span>
                    <span className="cnt">{failRows.length}</span>
                    {failsVisible ? <CaretUp size={12} className="text-faint" /> : <CaretDown size={12} className="text-faint" />}
                  </span>
                  <div className="line" />
                </div>

                {failsVisible && (
                  <div>
                    {failRows.map((r) => (
                      <ReleaseRow key={r.key} release={r} dim grabState={grabStateFor(r.key)} onGrab={() => grab(r)} />
                    ))}
                  </div>
                )}
              </>
            )}

            <div style={{ height: 14 }} />
          </div>

          <div className="search-panel-footer">
            <span className="text-faint" style={{ fontSize: 11.5 }}>
              Can't find the right release?{" "}
              <Link to={`/${resourceSegment}/${id}/grab`} style={{ textDecoration: "underline" }}>
                Add manually
              </Link>
            </span>
            <div style={{ flex: 1 }} />
            {!downloadClient && (
              <span className="text-ghost" style={{ fontFamily: "var(--font-mono)", fontSize: 10.5 }}>
                no download client configured
              </span>
            )}
          </div>
        </div>
      </div>

      {toast && (
        <div className="toast">
          <span className="toast-icon">
            <Check size={12} />
          </span>
          <span style={{ fontSize: 12.5 }}>{toast}</span>
        </div>
      )}
    </div>
  );
}
