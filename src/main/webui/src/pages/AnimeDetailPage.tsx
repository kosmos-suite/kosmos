import {
  ArrowLeftIcon as ArrowLeft,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  MagnifyingGlassIcon as MagnifyingGlass,
  SparkleIcon as Sparkle,
  StarIcon as Star,
} from "@phosphor-icons/react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { backdropUrl, posterUrl } from "../api/tmdbImage";
import { SimilarRow } from "../components/DetailExtrasSections";
import { useApi } from "../hooks/useApi";
import { useArtworkFallback } from "../hooks/useArtworkFallback";
import type { AnimeEpisode, EpisodeStatus } from "../api/types";

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

export default function AnimeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: anime, loading, error, reload } = useApi(() => api.getAnime(id!), [id]);
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const { data: extras } = useApi(() => api.getAnimeDetailExtras(id!), [id]);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);

  const { url: posterSrc, probe: posterProbe } = useArtworkFallback(
    anime ? posterUrl(anime.posterPath, "w500") : null,
    anime?.id,
    "poster",
  );
  const { url: backdropArt, probe: backdropProbe } = useArtworkFallback(
    anime ? backdropUrl(anime.backdropPath) : null,
    anime?.id,
    "backdrop",
  );

  if (loading) return <div className="page">Loading…</div>;
  if (error) return <div className="page text-muted">Failed to load: {error}</div>;
  if (!anime) return null;

  const activeProfile = profiles?.find((p) => p.id === anime.qualityProfileId) ?? null;

  async function setProfile(profileId: string | null) {
    if (!anime || profileSaving) return;
    setProfileSaving(true);
    setProfileMenuOpen(false);
    try {
      await api.updateAnimeQualityProfile(anime.id, profileId);
      await reload();
    } finally {
      setProfileSaving(false);
    }
  }

  return (
    <div>
      {backdropProbe}
      {posterProbe}
      <section
        className="detail-hero"
        style={
          backdropArt
            ? {
                height: 220,
                backgroundImage: `url(${backdropArt})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
              }
            : { height: 220 }
        }
      >
        <div className="detail-hero-topbar">
          <Link to="/" className="chip-floating">
            <ArrowLeft size={15} weight="bold" />
            Discover
          </Link>
        </div>
      </section>

      <div className="detail-body2">
        <div className="detail-poster2">
          <div className="detail-poster2-art">{posterSrc && <img src={posterSrc} alt="" />}</div>
        </div>

        <div className="detail-body2-main">
          <div className="detail-title-row">
            <span className="status-pill" style={{ background: "rgba(145,132,217,.16)", color: "#D2CEFD" }}>
              <Sparkle size={12} />
              {anime.status ?? "Anime"}
            </span>
          </div>

          <h1 className="detail-h1" style={{ fontSize: 38 }}>
            {anime.title}
          </h1>

          <div className="detail-meta-row2">
            {anime.year && <span>{anime.year}</span>}
            <span className="sep" />
            <span>
              {anime.episodeCountTotal ?? anime.episodes.length} episode{anime.episodeCountTotal === 1 ? "" : "s"}
            </span>
            {extras?.voteAverage != null && (
              <>
                <span className="sep" />
                <span style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Star size={12} weight="fill" color="#E0A94A" />
                  {extras.voteAverage.toFixed(1)} <span className="text-ghost">AniList</span>
                </span>
              </>
            )}
          </div>

          {extras && extras.genres.length > 0 && (
            <div className="detail-genres">
              {extras.genres.map((g) => (
                <span key={g} className="genre-tag">
                  {g}
                </span>
              ))}
            </div>
          )}

          {anime.overview && (
            <p className="detail-synopsis" style={{ maxWidth: "70ch" }}>
              {anime.overview}
            </p>
          )}

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
            Automatic search runs every 6 hours against a monitored episode's absolute number — matching into a
            batch release isn't supported yet, so a fansub group's single-episode releases are what gets found.
          </p>
        </div>
      </div>

      <div className="page">
        <div className="section-label" style={{ marginBottom: 12 }}>
          Episodes
        </div>
        {anime.episodes.map((episode) => (
          <EpisodeRow key={episode.id} episode={episode} />
        ))}
        {anime.episodes.length === 0 && <p className="text-muted">No episode data for this anime.</p>}

        {extras && extras.facts.length > 0 && (
          <div style={{ maxWidth: 420, marginTop: 32 }}>
            <div className="section-label">Details</div>
            <div className="fact-list">
              {extras.facts.map((f) => (
                <div key={f.k} className="fact-list-row">
                  <span className="k">{f.k}</span>
                  <span className="v">{f.v}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <SimilarRow items={extras?.similar ?? []} />
    </div>
  );
}

function EpisodeRow({ episode }: { episode: AnimeEpisode }) {
  const number = episode.absoluteEpisodeNumber ?? episode.episodeNumber;
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "48px 1fr auto 90px 32px",
        gap: 14,
        alignItems: "center",
        padding: "8px 4px",
        fontSize: 12.5,
        borderBottom: "1px solid var(--border)",
      }}
    >
      <span className="text-faint" style={{ fontFamily: "var(--font-mono)" }}>
        {number != null ? String(number).padStart(2, "0") : "—"}
      </span>
      <span>{episode.title}</span>
      <span className="status-tag" style={{ fontSize: 10.5 }}>
        <span className={`dot ${EPISODE_STATUS_DOT[episode.status]}`} />
        {EPISODE_STATUS_LABEL[episode.status]}
      </span>
      <span className="text-faint" style={{ fontSize: 11, textAlign: "right" }}>
        {episode.airDate ?? "—"}
      </span>
      <Link
        to={`/anime-episodes/${episode.id}/search`}
        className="btn btn-icon"
        title="Interactive search"
        style={{ width: 26, height: 26 }}
      >
        <MagnifyingGlass size={12} />
      </Link>
    </div>
  );
}
