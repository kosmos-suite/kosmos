import {
  ArrowLeftIcon as ArrowLeft,
  CaretDownIcon as CaretDown,
  CheckIcon as Check,
  DotsThreeIcon as DotsThree,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  InfoIcon as Info,
  ListMagnifyingGlassIcon as ListMagnifyingGlass,
  MagnifyingGlassIcon as MagnifyingGlass,
  PlayCircleIcon as PlayCircle,
  StarIcon as Star,
} from "@phosphor-icons/react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { backdropUrl, posterUrl } from "../api/tmdbImage";
import { CastRow, SimilarRow } from "../components/DetailExtrasSections";
import { useApi } from "../hooks/useApi";
import { useArtworkFallback } from "../hooks/useArtworkFallback";

function formatBytes(bytes: number): string {
  return `${(bytes / 1024 ** 3).toFixed(2)} GB`;
}

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.round((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}

function formatResolution(width: number | null, height: number | null): string {
  if (!height) return width && height ? `${width}×${height}` : "Unknown resolution";
  if (height >= 2000) return "2160p";
  if (height >= 1000) return "1080p";
  if (height >= 700) return "720p";
  return `${height}p`;
}

function relativeDays(iso: string): string {
  const days = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000));
  if (days === 0) return "today";
  if (days === 1) return "1 day ago";
  return `${days} days ago`;
}

export default function MovieDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: movie, loading, error, reload } = useApi(() => api.getMovie(id!), [id]);
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const { data: libraryFiles } = useApi(() => api.listMovieLibraryFiles(id!), [id]);
  const { data: extras } = useApi(() => api.getMovieDetailExtras(id!), [id]);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);

  const { url: posterSrc, probe: posterProbe } = useArtworkFallback(
    movie ? posterUrl(movie.posterPath, "w500") : null,
    movie?.id,
    "poster",
  );
  const { url: backdropArt, probe: backdropProbe } = useArtworkFallback(
    movie ? backdropUrl(movie.backdropPath) : null,
    movie?.id,
    "backdrop",
  );

  if (loading) return <div className="page">Loading…</div>;
  if (error) return <div className="page text-muted">Failed to load: {error}</div>;
  if (!movie) return null;

  const activeProfile = profiles?.find((p) => p.id === movie.qualityProfileId) ?? null;
  const file = libraryFiles?.[0] ?? null;

  async function setProfile(profileId: string | null) {
    if (!movie || profileSaving) return;
    setProfileSaving(true);
    setProfileMenuOpen(false);
    try {
      await api.updateMovieQualityProfile(movie.id, profileId);
      await reload();
    } finally {
      setProfileSaving(false);
    }
  }

  const director = extras?.facts.find((f) => f.k === "Director")?.v;

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
        <div className="detail-hero-topbar">
          <Link to="/" className="chip-floating">
            <ArrowLeft size={15} weight="bold" />
            Discover
          </Link>
        </div>
        <span className="chip-floating detail-hero-trailer" title="No trailer source wired up yet">
          <PlayCircle size={16} weight="fill" />
          Trailer
        </span>
      </section>

      <div className="detail-body2">
        <div className="detail-poster2">
          <div className="detail-poster2-art">
            {posterSrc && <img src={posterSrc} alt="" />}
          </div>
        </div>

        <div className="detail-body2-main">
          <div className="detail-title-row">
            <span className={`status-pill ${file ? "good" : "bad"}`}>
              <span className="dot" />
              {file ? "In Library" : "Missing"}
            </span>
            <span className="detail-title-note">
              {activeProfile ? `monitored · automatic search every 6h against "${activeProfile.name}"` : "not monitored"}
            </span>
          </div>

          <h1 className="detail-h1">{movie.title}</h1>

          <div className="detail-meta-row2">
            {movie.year && <span>{movie.year}</span>}
            <span className="sep" />
            <span>{movie.runtimeMinutes ? `${Math.floor(movie.runtimeMinutes / 60)}h ${movie.runtimeMinutes % 60}m` : "Runtime unknown"}</span>
            {extras?.certification && (
              <>
                <span className="sep" />
                <span className="cert-badge">{extras.certification}</span>
              </>
            )}
            {extras?.voteAverage != null && (
              <>
                <span className="sep" />
                <span style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Star size={12} weight="fill" color="#E0A94A" />
                  {extras.voteAverage.toFixed(1)} <span className="text-ghost">TMDB</span>
                </span>
              </>
            )}
            {director && (
              <>
                <span className="sep" />
                <span>{director}</span>
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

          <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
              <Link to={`/movies/${movie.id}/search`} className="btn btn-hero">
                <MagnifyingGlass size={16} weight="bold" />
                Search Now
              </Link>
              <Link to={`/movies/${movie.id}/search`} className="btn btn-secondary">
                <ListMagnifyingGlass size={15} />
                Interactive search
              </Link>
              <div className="dropdown-wrap">
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
                    <div
                      className={`grab-client-item${!activeProfile ? " active" : ""}`}
                      onClick={() => setProfile(null)}
                    >
                      <EyeSlash size={14} className="text-muted" />
                      <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>Not monitored</span>
                      {!activeProfile && <Check size={12} color="#B5ABFC" />}
                    </div>
                    {profiles?.map((p) => (
                      <div
                        key={p.id}
                        className={`grab-client-item${p.id === activeProfile?.id ? " active" : ""}`}
                        onClick={() => setProfile(p.id)}
                      >
                        <Eye size={14} className="text-muted" />
                        <span style={{ flex: 1, minWidth: 0, fontSize: 12.5 }}>{p.name}</span>
                        {p.id === activeProfile?.id && <Check size={12} color="#B5ABFC" />}
                      </div>
                    ))}
                  </div>
                )}
              </div>
              <button type="button" className="btn btn-icon">
                <DotsThree size={17} />
              </button>
            </div>

            {file ? (
              <div className="info-card">
                <div className="info-card-header">
                  <span className="dot dot-good" />
                  <span style={{ fontWeight: 500, fontSize: 13.5, color: "#8FCB9B" }}>
                    In library — file on disk
                  </span>
                  <div style={{ flex: 1 }} />
                  <span className="text-faint" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                    imported {relativeDays(file.importedAt)}
                  </span>
                </div>
                <div className="info-card-facts cols-4">
                  <div className="info-card-fact">
                    <div className="k">Quality</div>
                    <div className="v">
                      {formatResolution(file.resolutionWidth, file.resolutionHeight)}
                      {file.videoCodec ? ` ${file.videoCodec}` : ""}
                      {file.hdrFormat ? ` · ${file.hdrFormat}` : ""}
                    </div>
                  </div>
                  <div className="info-card-fact">
                    <div className="k">Size</div>
                    <div className="v">{formatBytes(file.sizeBytes)}</div>
                  </div>
                  <div className="info-card-fact">
                    <div className="k">Duration</div>
                    <div className="v">{file.durationSeconds ? formatDuration(file.durationSeconds) : "Not probed"}</div>
                  </div>
                  <div className="info-card-fact">
                    <div className="k">Container</div>
                    <div className="v">{file.container ?? "Not probed"}</div>
                  </div>
                </div>
                <div className="info-card-footer">
                  <Info size={14} className="text-faint" />
                  <span className="info-card-path">{file.path}</span>
                </div>
              </div>
            ) : (
              <div className="info-card bad">
                <div className="info-card-header">
                  <span className="dot dot-bad" />
                  <span style={{ fontWeight: 500, fontSize: 13.5, color: "#EE9891" }}>Missing — no file on disk</span>
                  <div style={{ flex: 1 }} />
                  <span className="text-faint" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>
                    added {relativeDays(movie.addedAt)}
                  </span>
                </div>
                <div className="info-card-facts cols-3">
                  <div className="info-card-fact">
                    <div className="k">Quality profile</div>
                    <div className="v">{activeProfile ? activeProfile.name : "None"}</div>
                  </div>
                </div>
                <div className="info-card-footer">
                  <Info size={14} className="text-faint" />
                  <span style={{ flex: 1 }}>
                    {activeProfile
                      ? `Kosmos checks every enabled indexer against "${activeProfile.name}" every 6 hours until this scores above cutoff.`
                      : "Assign a quality profile above to let Kosmos search for this automatically, or search yourself now."}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="page">
        <div className="detail-info-grid">
          <div>
            <div className="section-label">Synopsis</div>
            {movie.overview && <p className="detail-synopsis">{movie.overview}</p>}
          </div>
          {extras && extras.facts.length > 0 && (
            <div>
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

        <CastRow cast={extras?.cast ?? []} />
        <SimilarRow items={extras?.similar ?? []} />
      </div>
    </div>
  );
}
