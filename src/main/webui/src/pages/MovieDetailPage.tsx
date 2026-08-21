import {
  ArrowLeftIcon as ArrowLeft,
  CaretDownIcon as CaretDown,
  CaretLeftIcon as CaretLeft,
  CaretRightIcon as CaretRight,
  CheckIcon as Check,
  DotsThreeIcon as DotsThree,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  InfoIcon as Info,
  ListMagnifyingGlassIcon as ListMagnifyingGlass,
  MagnifyingGlassIcon as MagnifyingGlass,
  PlayIcon as Play,
  PlayCircleIcon as PlayCircle,
  PlusIcon as Plus,
  StarIcon as Star,
} from "@phosphor-icons/react";
import { useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { backdropUrl, posterUrl } from "../api/tmdbImage";
import { useApi } from "../hooks/useApi";
import {
  cast,
  certification,
  detailFacts,
  genres,
  similarTitles,
  tmdbRating,
} from "../mocks/movieDetailExtras";

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

/**
 * Cast, similar titles, genres, and certification have no real backend source yet — TMDB's
 * credits/similar/details endpoints are never fetched, only search results are (see
 * mocks/movieDetailExtras.ts). Library status and file/probe facts below ARE real: they come
 * from GET /movies/{id}/library-files, now that a LibraryFile-to-Movie link exists.
 */
export default function MovieDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: movie, loading, error, reload } = useApi(() => api.getMovie(id!), [id]);
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const { data: libraryFiles } = useApi(() => api.listMovieLibraryFiles(id!), [id]);
  const similarRowRef = useRef<HTMLDivElement>(null);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);

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

  const posterSrc = posterUrl(movie.posterPath, "w500");

  const scrollSimilar = (dir: 1 | -1) => {
    const el = similarRowRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: "smooth" });
  };

  return (
    <div>
      <section
        className="detail-hero"
        style={
          movie.backdropPath
            ? {
                backgroundImage: `url(${backdropUrl(movie.backdropPath)})`,
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
            <span className="sep" />
            <span className="cert-badge">{certification}</span>
            <span className="sep" />
            <span style={{ display: "inline-flex", alignItems: "center", gap: 5 }}>
              <Star size={12} weight="fill" color="#E0A94A" />
              {tmdbRating} <span className="text-ghost">TMDB</span>
            </span>
            <span className="sep" />
            <span>{detailFacts[0].v}</span>
          </div>

          <div className="detail-genres">
            {genres.map((g) => (
              <span key={g} className="genre-tag">
                {g}
              </span>
            ))}
          </div>

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
          <div>
            <div className="section-label">Details</div>
            <div className="fact-list">
              {detailFacts.map((f) => (
                <div key={f.k} className="fact-list-row">
                  <span className="k">{f.k}</span>
                  <span className="v">{f.v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="content-row">
          <div className="content-row-header">
            <h2>Cast</h2>
            <span className="content-row-sub">top billed · 14 credited</span>
            <div style={{ flex: 1 }} />
            <span style={{ fontSize: 11.5, color: "var(--text-muted)", cursor: "pointer" }}>Full cast &amp; crew</span>
          </div>
          <div className="cast-row k-scroll">
            {cast.map((p) => (
              <div key={p.name} className="cast-item">
                <div className="cast-avatar">
                  <span>{p.initials}</span>
                </div>
                <div className="cast-name">{p.name}</div>
                <div className="cast-role">{p.role}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="content-row" style={{ paddingBottom: 40 }}>
          <div className="content-row-header">
            <h2>More Like This</h2>
            <span className="content-row-sub">TMDB similar · 9 titles</span>
            <div style={{ flex: 1 }} />
            <button type="button" className="row-scroll-btn" onClick={() => scrollSimilar(-1)}>
              <CaretLeft size={15} />
            </button>
            <button type="button" className="row-scroll-btn" onClick={() => scrollSimilar(1)}>
              <CaretRight size={15} />
            </button>
          </div>
          <div className="poster-row k-scroll" ref={similarRowRef}>
            {similarTitles.map((s) => {
              const owned = s.status === "library";
              return (
                <div key={s.title} className="similar-card">
                  <div className="similar-card-art">
                    <div
                      className="similar-card-badge"
                      style={{
                        background: s.status === "new" ? "rgba(145,132,217,.2)" : "rgba(11,12,18,.7)",
                        border: `1px solid ${s.status === "new" ? "rgba(145,132,217,.42)" : "rgba(233,233,237,.1)"}`,
                        color: s.status === "new" ? "#D2CEFD" : "#E9E9ED",
                      }}
                    >
                      {s.status !== "new" && (
                        <span className={`dot ${s.status === "library" ? "dot-good" : "dot-bad"}`} />
                      )}
                      {s.status === "library" ? "IN LIBRARY" : s.status === "missing" ? "MISSING" : "TMDB"}
                    </div>
                    <div className="similar-card-scrim" />
                    <div className="similar-card-action">
                      <span
                        style={{
                          background: owned ? "rgba(233,233,237,.16)" : "var(--accent-gradient)",
                          color: owned ? "#E9E9ED" : "#0B0C12",
                          border: owned ? "1px solid rgba(233,233,237,.14)" : "0",
                        }}
                      >
                        {owned ? <Play size={13} weight="fill" /> : <Plus size={13} />}
                        {owned ? "Play" : "Add"}
                      </span>
                    </div>
                  </div>
                  <div className="similar-card-title">{s.title}</div>
                  <div className="similar-card-meta">{s.meta}</div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
