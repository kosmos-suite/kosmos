import { ArrowLeftIcon as ArrowLeft } from "@phosphor-icons/react";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import type { DiscoverItem } from "../api/types";
import { useAddToLibrary } from "../hooks/useAddToLibrary";
import { dedupeDiscoverItems } from "../utils/dedupeDiscoverItems";
import { discoverItemLink } from "../utils/discoverItemLink";
import { tonalGradient } from "../utils/tonalGradient";
import { MediaCard } from "./MediaCard";

const PAGE_SIZE = 20;

interface DiscoverGridProps {
  title: string;
  fetcher: (page: number) => Promise<DiscoverItem[]>;
  /** Identifies which route params/filters this fetcher was built from — a fetcher closure isn't a
   * stable dep by itself, so this is what actually drives resetting back to page 1 and refetching. */
  depKey: string;
  /** Optional filter controls (e.g. Trending's window/type toggles) rendered next to the title. */
  filters?: ReactNode;
}

/** Grid click-through target for a Discover/Home row's "See all" — paginates via infinite scroll. */
export function DiscoverGrid({ title, fetcher, depKey, filters }: DiscoverGridProps) {
  const { stateFor, triggerAdd } = useAddToLibrary();
  const [items, setItems] = useState<DiscoverItem[]>([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setItems([]);
    setPage(1);
    setHasMore(true);

    fetcherRef
      .current(1)
      .then((result) => {
        if (cancelled) return;
        setItems(dedupeDiscoverItems(result));
        setHasMore(result.length >= PAGE_SIZE);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [depKey]);

  function loadMore() {
    if (loading || loadingMore || !hasMore) return;
    const nextPage = page + 1;
    setLoadingMore(true);
    fetcherRef
      .current(nextPage)
      .then((result) => {
        setItems((current) => dedupeDiscoverItems([...current, ...result]));
        setPage(nextPage);
        setHasMore(result.length >= PAGE_SIZE);
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setLoadingMore(false));
  }

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) loadMore();
      },
      { rootMargin: "600px" },
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [depKey, hasMore, loading, loadingMore]);

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
      <div className="discover-grid-header">
        <h1 className="discover-grid-title">{title}</h1>
        {filters && <div className="discover-grid-filters">{filters}</div>}
      </div>

      {loading && <p className="text-muted">Loading…</p>}
      {error && <p className="text-muted">Couldn't load — {error}</p>}
      {!loading && items.length === 0 && !error && <p className="text-muted">Nothing here yet.</p>}

      <div className="poster-grid">
        {items.map((item, i) => (
          <MediaCard
            key={`${item.mediaItemId ?? item.externalId}-${i}`}
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
                    triggerAdd({
                      externalId: item.externalId!,
                      title: item.title,
                      year: item.year,
                      overview: item.overview,
                      posterPath: item.posterPath,
                      backdropPath: item.backdropPath,
                      mediaType: item.mediaType,
                    })
            }
            addState={item.externalId ? stateFor(item.externalId) : undefined}
          />
        ))}
      </div>

      <div ref={sentinelRef} />
      {loadingMore && (
        <p className="text-muted" style={{ textAlign: "center", padding: "20px 0" }}>
          Loading more…
        </p>
      )}
    </div>
  );
}
