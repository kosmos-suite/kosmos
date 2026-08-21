import { ArrowLeftIcon as ArrowLeft } from "@phosphor-icons/react";
import { Link } from "react-router-dom";
import type { DiscoverItem } from "../api/types";
import { useApi } from "../hooks/useApi";
import { discoverItemLink } from "../utils/discoverItemLink";
import { tonalGradient } from "../utils/tonalGradient";
import { MovieCard } from "./MovieCard";

interface DiscoverGridProps {
  title: string;
  fetcher: () => Promise<DiscoverItem[]>;
  /** Identifies which route params this fetcher was built from, so a tile-to-tile navigation on
   * the same route path (params-only change) actually refetches instead of reusing stale data. */
  depKey: string;
}

/** Grid click-through target for a Discover/Home tile row (genre, studio, network). */
export function DiscoverGrid({ title, fetcher, depKey }: DiscoverGridProps) {
  const { data, loading, error } = useApi(fetcher, [depKey]);

  return (
    <div className="page">
      <div className="page-header" style={{ padding: "0 0 4px", display: "block" }}>
        <Link
          to="/"
          className="text-muted"
          style={{ fontSize: 12.5, display: "inline-flex", alignItems: "center", gap: 6, marginBottom: 10 }}
        >
          <ArrowLeft size={13} />
          Discover
        </Link>
        <h2 style={{ marginBottom: 0 }}>{title}</h2>
      </div>

      {loading && <p className="text-muted">Loading…</p>}
      {error && <p className="text-muted">Couldn't load — {error}</p>}
      {data?.length === 0 && <p className="text-muted">Nothing here yet.</p>}

      <div className="poster-grid">
        {data?.map((item, i) => (
          <MovieCard
            key={item.mediaItemId ?? item.externalId}
            to={discoverItemLink(item)}
            title={item.title}
            year={item.year}
            posterPath={item.posterPath}
            mediaItemId={item.mediaItemId}
            status={item.inLibrary ? "in-library" : undefined}
            placeholderBackground={tonalGradient(i)}
          />
        ))}
      </div>
    </div>
  );
}
