import { ArrowLeftIcon as ArrowLeft } from "@phosphor-icons/react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import { MediaCard } from "../components/MediaCard";
import { QualityProfileDropdown } from "../components/detail/QualityProfileDropdown";
import type { QualityProfile } from "../api/types";
import { tonalGradient } from "../utils/tonalGradient";

export default function CollectionPage() {
  const { tmdbId } = useParams<{ tmdbId: string }>();
  const { data: detail, error, reload } = useApi(() => api.getMovieCollection(tmdbId!), [tmdbId]);
  const { data: profiles } = useApi(() => api.listQualityProfiles(), []);
  const [saving, setSaving] = useState(false);

  const activeProfile = null; // set at monitor time only — collections don't surface it on this view yet

  async function monitor(profileId: string | null) {
    if (!tmdbId || saving) return;
    setSaving(true);
    try {
      await api.monitorMovieCollection(tmdbId, profileId);
      reload();
    } finally {
      setSaving(false);
    }
  }

  async function unmonitor() {
    if (!detail?.movieCollectionId || saving) return;
    setSaving(true);
    try {
      await api.unmonitorMovieCollection(detail.movieCollectionId);
      reload();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page">
      <Link
        to="/"
        className="text-muted"
        style={{ fontSize: 12.5, display: "inline-flex", alignItems: "center", gap: 6, marginBottom: 10 }}
      >
        <ArrowLeft size={13} />
        Discover
      </Link>

      {error && <p className="text-muted">Couldn't load collection — {error}</p>}

      {detail && (
        <>
          <div className="discover-grid-header">
            <h1 className="discover-grid-title">{detail.name}</h1>
            {detail.monitored ? (
              <button type="button" className="btn btn-secondary" onClick={unmonitor} disabled={saving}>
                {saving ? "Saving…" : "Unmonitor"}
              </button>
            ) : (
              <QualityProfileDropdown
                profiles={profiles as QualityProfile[] | null}
                activeProfile={activeProfile}
                onSelect={monitor}
              />
            )}
          </div>
          <p className="text-muted" style={{ maxWidth: "60ch", marginBottom: 20 }}>
            {detail.monitored
              ? "Monitored — missing entries are periodically filed as Requests for approval."
              : "Pick a quality profile to monitor this collection. New/missing entries go to Requests, same as any other automated source."}
          </p>

          <div className="poster-grid">
            {detail.members.map((member, i) => (
              <MediaCard
                key={member.externalId}
                to={member.inLibrary && member.mediaItemId ? `/movies/${member.mediaItemId}` : `/movies/tmdb/${member.externalId}`}
                title={member.title}
                year={member.year}
                posterPath={member.posterPath}
                mediaItemId={member.mediaItemId}
                status={member.inLibrary ? "in-library" : "missing"}
                placeholderBackground={tonalGradient(i)}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
