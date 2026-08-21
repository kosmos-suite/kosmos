import {
  CaretDownIcon as CaretDown,
  CaretUpIcon as CaretUp,
  CheckCircleIcon as CheckCircle,
  CheckIcon as Check,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  MagnifyingGlassIcon as MagnifyingGlass,
  PlayCircleIcon as PlayCircle,
  PlusIcon as Plus,
  SpinnerIcon as Spinner,
  StarIcon as Star,
  TelevisionIcon as Television,
} from "@phosphor-icons/react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { backdropUrl, posterUrl } from "../api/tmdbImage";
import { CastRow, SimilarRow } from "../components/DetailExtrasSections";
import { useAddToLibrary } from "../hooks/useAddToLibrary";
import { useApi } from "../hooks/useApi";
import { useArtworkFallback } from "../hooks/useArtworkFallback";
import type { Episode, EpisodeStatus, PreviewSeason, Season } from "../api/types";

const EPISODE_STATUS_DOT: Record<EpisodeStatus, string> = {
  MISSING: "dot-bad",
  GRABBED: "dot-warn",
  IMPORTED: "dot-warn",
  AVAILABLE: "dot-good",
};
const EPISODE_STATUS_LABEL: Record<EpisodeStatus, string> = {
  MISSING: "Missing",
  GRABBED: "Grabbed",
  IMPORTED: "Importing",
  AVAILABLE: "Available",
};

/**
 * Not-owned previews carry the same season/episode tree TMDB gives an owned show, just without a
 * real {@code Season}/{@code Episode} row for it yet — adapted into that shape here so {@code
 * SeasonRow}/{@code EpisodeRow} render it identically, with every episode implicitly "missing".
 */
function seasonsFromPreview(seasons: PreviewSeason[]): Season[] {
  return seasons.map((s) => ({
    id: `preview-season-${s.seasonNumber}`,
    seasonNumber: s.seasonNumber,
    name: s.name,
    overview: null,
    posterPath: null,
    episodeCount: s.episodeCount,
    episodes: s.episodes.map((e) => ({
      id: `preview-episode-${s.seasonNumber}-${e.episodeNumber}`,
      episodeNumber: e.episodeNumber,
      title: e.title,
      overview: null,
      airDate: e.airDate,
      runtimeMinutes: null,
      stillPath: null,
      status: "MISSING" as EpisodeStatus,
    })),
  }));
}

/**
 * Doubles as the "not in library" preview screen (route {@code /shows/tmdb/:externalId}) — a
 * not-owned card links here instead of falling back to a search. See {@code MovieDetailPage}'s
 * own doc comment for the same pattern applied there.
 */
export default function ShowDetailPage() {
  const { id, externalId } = useParams<{ id?: string; externalId?: string }>();
  const owned = !!id;

  const { data: show, loading: showLoading, error: showError, reload } = useApi(
    () => (id ? api.getShow(id) : Promise.resolve(null)),
    [id],
  );
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const { data: extras } = useApi(
    () => (id ? api.getShowDetailExtras(id) : Promise.resolve(null)),
    [id],
  );
  const {
    data: preview,
    loading: previewLoading,
    error: previewError,
  } = useApi(() => (externalId ? api.getShowPreview(externalId) : Promise.resolve(null)), [externalId]);
  const { admin, stateFor, triggerAdd } = useAddToLibrary();
  const [openSeasonId, setOpenSeasonId] = useState<string | null>(null);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);

  const title = show?.title ?? preview?.title ?? "";
  const year = show?.year ?? preview?.year ?? null;
  const overview = show?.overview ?? preview?.overview ?? null;
  const posterPath = show?.posterPath ?? preview?.posterPath ?? null;
  const backdropPath = show?.backdropPath ?? preview?.backdropPath ?? null;
  const genres = extras?.genres ?? preview?.genres ?? [];
  const facts = extras?.facts ?? preview?.facts ?? [];
  const cast = extras?.cast ?? preview?.cast ?? [];
  const similar = extras?.similar ?? preview?.similar ?? [];
  const voteAverage = extras?.voteAverage ?? preview?.voteAverage ?? null;
  const certification = extras?.certification ?? preview?.certification ?? null;
  const trailerUrl = extras?.trailerUrl ?? preview?.trailerUrl ?? null;
  const seasons = owned && show ? show.seasons : seasonsFromPreview(preview?.seasons ?? []);

  const { url: posterSrc, probe: posterProbe } = useArtworkFallback(
    posterUrl(posterPath, "w500"),
    show?.id,
    "poster",
  );
  const { url: backdropArt, probe: backdropProbe } = useArtworkFallback(
    backdropUrl(backdropPath),
    show?.id,
    "backdrop",
  );

  if (showLoading || previewLoading) return <div className="page">Loading…</div>;
  if (owned && showError) return <div className="page text-muted">Failed to load: {showError}</div>;
  if (!owned && previewError) return <div className="page text-muted">Failed to load: {previewError}</div>;
  if (owned && !show) return null;
  if (!owned && !preview) return null;

  const activeProfile = profiles?.find((p) => p.id === show?.qualityProfileId) ?? null;

  const episodes = show?.seasons.flatMap((s) => s.episodes) ?? [];
  const availableEpisodes = episodes.filter((e) => e.status === "AVAILABLE").length;
  const availability: "good" | "warn" | "bad" =
    availableEpisodes === 0 ? "bad" : availableEpisodes === episodes.length ? "good" : "warn";
  const availabilityLabel =
    availability === "good" ? "In Library" : availability === "warn" ? "Partially Available" : "Missing";

  async function setProfile(profileId: string | null) {
    if (!show || profileSaving) return;
    setProfileSaving(true);
    setProfileMenuOpen(false);
    try {
      await api.updateShowQualityProfile(show.id, profileId);
      await reload();
    } finally {
      setProfileSaving(false);
    }
  }

  const addState = preview ? stateFor(preview.externalId) : "idle";
  const addLabel =
    addState === "adding"
      ? admin
        ? "Adding…"
        : "Requesting…"
      : addState === "added"
        ? admin
          ? "Added"
          : "Requested"
        : admin
          ? "Add to Library"
          : "Request";

  return (
    <div>
      {backdropProbe}
      {posterProbe}
      <section
        className="detail-hero"
        style={
          backdropArt
            ? {
                backgroundImage: `url(${backdropArt})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
              }
            : undefined
        }
      >
        {trailerUrl && (
          <a
            href={trailerUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="chip-floating detail-hero-trailer"
          >
            <PlayCircle size={20} weight="fill" />
            Trailer
          </a>
        )}
      </section>

      <div className="detail-body2">
        <div className="detail-poster2">
          <div className="detail-poster2-art">{posterSrc && <img src={posterSrc} alt="" />}</div>
        </div>

        <div className="detail-body2-main">
          <div className="detail-title-row">
            {owned ? (
              <>
                <span className={`status-pill ${availability}`}>
                  <span className="dot" />
                  {availabilityLabel}
                </span>
                <span className="detail-title-note">
                  {activeProfile
                    ? `monitored · automatic search every 6h against "${activeProfile.name}"`
                    : "not monitored"}
                </span>
              </>
            ) : (
              <span className="status-pill accent">Not in Library</span>
            )}
          </div>

          <h1 className="detail-h1" style={{ fontSize: 38 }}>
            {title}
          </h1>

          <div className="detail-meta-row2">
            {year && <span>{year}</span>}
            {seasons.length > 0 && (
              <>
                <span className="sep" />
                <span>{seasons.length} season{seasons.length === 1 ? "" : "s"}</span>
                <span className="sep" />
                <span>{seasons.reduce((sum, s) => sum + s.episodes.length, 0)} episodes</span>
              </>
            )}
            {owned && show?.status && (
              <>
                <span className="sep" />
                <span style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Television size={12} />
                  {show.status}
                </span>
              </>
            )}
            {certification && (
              <>
                <span className="sep" />
                <span className="cert-badge">{certification}</span>
              </>
            )}
            {voteAverage != null && (
              <>
                <span className="sep" />
                <span style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Star size={12} weight="fill" color="#E0A94A" />
                  {voteAverage.toFixed(1)} <span className="text-ghost">TMDB</span>
                </span>
              </>
            )}
          </div>

          {genres.length > 0 && (
            <div className="detail-genres">
              {genres.map((g) => (
                <span key={g} className="genre-tag">
                  {g}
                </span>
              ))}
            </div>
          )}

          {owned ? (
            <>
              <div className="dropdown-wrap" style={{ marginTop: 14 }}>
                <button
                  type="button"
                  className={`btn btn-secondary${profileMenuOpen ? " open" : ""}`}
                  disabled={profileSaving}
                  onClick={() => setProfileMenuOpen((o) => !o)}
                >
                  {activeProfile ? <Eye size={15} /> : <EyeSlash size={15} />}
                  {profileSaving ? "Saving…" : activeProfile ? activeProfile.name : "Not monitored"}
                  <CaretDown size={11} className="text-faint" />
                </button>
                {profileMenuOpen && (
                  <div className="grab-client-menu">
                    <div className={`grab-client-item${!activeProfile ? " active" : ""}`} onClick={() => setProfile(null)}>
                      <EyeSlash size={14} className="text-muted" />
                      <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>Not monitored</span>
                      {!activeProfile && <Check size={12} color="#B5ABFC" />}
                    </div>
                    {profiles?.map((p) => (
                      <div key={p.id} className={`grab-client-item${p.id === activeProfile?.id ? " active" : ""}`} onClick={() => setProfile(p.id)}>
                        <Eye size={14} className="text-muted" />
                        <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>{p.name}</span>
                        {p.id === activeProfile?.id && <Check size={12} color="#B5ABFC" />}
                      </div>
                    ))}
                  </div>
                )}
              </div>
              <p className="text-faint" style={{ fontSize: 11.5, marginTop: 8 }}>
                {activeProfile
                  ? `Kosmos checks every enabled indexer against "${activeProfile.name}" every 6 hours, per missing episode.`
                  : "Assign a quality profile to let Kosmos search for missing episodes automatically."}
              </p>
            </>
          ) : (
            <button
              type="button"
              className="btn btn-secondary"
              style={{ marginTop: 14 }}
              disabled={addState !== "idle"}
              onClick={() =>
                preview &&
                triggerAdd({
                  externalId: preview.externalId,
                  title: preview.title,
                  year: preview.year,
                  overview: preview.overview,
                  posterPath: preview.posterPath,
                  backdropPath: preview.backdropPath,
                  mediaType: "tv",
                })
              }
            >
              {addState === "adding" ? (
                <Spinner size={16} className="spin" />
              ) : addState === "added" ? (
                <CheckCircle size={16} weight="fill" />
              ) : (
                <Plus size={16} weight="bold" />
              )}
              {addLabel}
            </button>
          )}
        </div>
      </div>

      <div className="page">
        <div className="detail-info-grid">
          <div>
            <div className="section-label">Synopsis</div>
            {overview && <p className="detail-synopsis">{overview}</p>}
          </div>
          {facts.length > 0 && (
            <div>
              <div className="section-label">Details</div>
              <div className="fact-list">
                {facts.map((f) => (
                  <div key={f.k} className="fact-list-row">
                    <span className="k">{f.k}</span>
                    <span className="v">{f.v}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {seasons.length > 0 && (
          <div style={{ marginTop: 38 }}>
            <div className="section-label" style={{ marginBottom: 12 }}>
              Seasons
            </div>

            {seasons.map((season) => (
              <SeasonRow
                key={season.id}
                season={season}
                owned={owned}
                open={openSeasonId === season.id}
                onToggle={() => setOpenSeasonId((s) => (s === season.id ? null : season.id))}
              />
            ))}
          </div>
        )}

        <CastRow cast={cast} />
        <SimilarRow items={similar} />
      </div>
    </div>
  );
}

function SeasonRow({
  season,
  owned,
  open,
  onToggle,
}: {
  season: Season;
  owned: boolean;
  open: boolean;
  onToggle: () => void;
}) {
  return (
    <div style={{ borderBottom: "1px solid var(--border)" }}>
      <div
        onClick={onToggle}
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "13px 4px",
          cursor: "pointer",
        }}
      >
        <span style={{ fontWeight: 500, fontSize: 13.5, flex: 1 }}>{season.name}</span>
        <span className="text-faint" style={{ fontSize: 11.5 }}>
          {season.episodeCount ?? season.episodes.length} episodes
        </span>
        {open ? <CaretUp size={13} className="text-faint" /> : <CaretDown size={13} className="text-faint" />}
      </div>

      {open && (
        <div style={{ paddingBottom: 12 }}>
          {season.episodes.map((ep) => (
            <EpisodeRow key={ep.id} episode={ep} owned={owned} />
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

function EpisodeRow({ episode, owned }: { episode: Episode; owned: boolean }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "48px 1fr auto 90px 32px",
        gap: 14,
        alignItems: "center",
        padding: "8px 4px",
        fontSize: 12.5,
      }}
    >
      <span className="text-faint" style={{ fontFamily: "var(--font-mono)" }}>
        {String(episode.episodeNumber).padStart(2, "0")}
      </span>
      <span>{episode.title}</span>
      <span className="status-tag" style={{ fontSize: 10.5 }}>
        <span className={`dot ${EPISODE_STATUS_DOT[episode.status]}`} />
        {EPISODE_STATUS_LABEL[episode.status]}
      </span>
      <span className="text-faint" style={{ fontSize: 11, textAlign: "right" }}>
        {episode.airDate ?? "—"}
      </span>
      {owned ? (
        <Link
          to={`/episodes/${episode.id}/search`}
          className="btn btn-icon"
          title="Interactive search"
          style={{ width: 26, height: 26 }}
        >
          <MagnifyingGlass size={12} />
        </Link>
      ) : (
        <span />
      )}
    </div>
  );
}
