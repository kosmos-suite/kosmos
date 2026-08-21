import {
  CaretLeftIcon as CaretLeft,
  CaretRightIcon as CaretRight,
  PlusIcon as Plus,
  StarIcon as Star,
  TrendUpIcon as TrendUp,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { backdropUrl, logoUrl } from "../api/tmdbImage";
import type { DiscoverItem, GenreTile as GenreTileType, StudioTile as StudioTileType } from "../api/types";
import { MediaCard } from "../components/MediaCard";
import { useAddToLibrary } from "../hooks/useAddToLibrary";
import { useApi } from "../hooks/useApi";
import { useArtworkFallback } from "../hooks/useArtworkFallback";
import { discoverItemLink } from "../utils/discoverItemLink";
import { tonalGradient } from "../utils/tonalGradient";

const HERO_SLIDE_COUNT = 10;
const HERO_INTERVAL_MS = 7600;

function HeroSlideArt({ slide, index }: { slide: DiscoverItem; index: number }) {
  const tmdbArt = backdropUrl(slide.backdropPath, "original");
  const { url, probe } = useArtworkFallback(tmdbArt, slide.mediaItemId, "backdrop");

  return (
    <>
      <div
        className="hero-slide-art"
        style={
          url
            ? { backgroundImage: `url(${url})`, backgroundSize: "cover", backgroundPosition: "center" }
            : { background: tonalGradient(index) }
        }
      />
      {probe}
    </>
  );
}

/**
 * Real trending (falling back to popular if trending is empty) — no recommendation engine or
 * per-user curation exists yet, so this is just "pick some from what TMDB says is hot right now."
 */
function Hero() {
  const { data: trending } = useApi(api.discoverTrending);
  const { data: popular } = useApi(api.discoverPopular);
  const [active, setActive] = useState(0);
  const [paused, setPaused] = useState(false);

  const { slides, sourceLabel } = useMemo(() => {
    if (trending && trending.length > 0) {
      return { slides: trending.slice(0, HERO_SLIDE_COUNT), sourceLabel: "Trending" };
    }
    if (popular && popular.length > 0) {
      return { slides: popular.slice(0, HERO_SLIDE_COUNT), sourceLabel: "Popular" };
    }
    return { slides: [] as DiscoverItem[], sourceLabel: "Trending" };
  }, [trending, popular]);

  useEffect(() => {
    if (paused || slides.length < 2) return;
    const timer = setInterval(() => {
      setActive((i) => (i + 1) % slides.length);
    }, HERO_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [paused, slides.length]);

  useEffect(() => {
    if (active >= slides.length) setActive(0);
  }, [slides.length, active]);

  if (slides.length === 0) return null;
  const slide = slides[active];

  return (
    <div className="hero" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="hero-slides">
        {slides.map((s, i) => (
          <div key={s.mediaItemId ?? s.externalId} className={`hero-slide${i === active ? " active" : ""}`}>
            <HeroSlideArt slide={s} index={i} />
          </div>
        ))}
      </div>

      <div className="hero-content">
        <div className="hero-badges">
          <span className="hero-kicker">
            <TrendUp size={12} weight="bold" />
            {sourceLabel} #{active + 1}
          </span>
          <span className="hero-rank">{slide.inLibrary ? "Already in your library" : "Not in your library"}</span>
        </div>
        <h1 className="hero-title">{slide.title}</h1>
        <div className="hero-meta">
          {slide.year && <span>{slide.year}</span>}
          {slide.voteAverage != null && (
            <>
              <span className="text-faint">·</span>
              <span className="hero-rating">
                <Star size={12} weight="fill" />
                {slide.voteAverage.toFixed(1)}
              </span>
            </>
          )}
        </div>
        <p className="hero-synopsis">{slide.overview}</p>
        <div className="hero-actions">
          <Link to={discoverItemLink(slide)} className="btn btn-hero">
            <Plus size={16} weight="bold" />
            {slide.inLibrary ? "View in library" : "Add to library"}
          </Link>
        </div>
      </div>

      <div className="hero-corner">
        <div className="hero-counter">
          {active + 1} / {slides.length}
        </div>
        <div className="hero-dots">
          {slides.map((s, i) => (
            <button
              key={s.mediaItemId ?? s.externalId}
              type="button"
              className={`hero-dot${i === active ? " active" : ""}`}
              onClick={() => setActive(i)}
              aria-label={`Show ${s.title}`}
            >
              {i === active && <span className="hero-dot-fill" />}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

interface RowShellProps {
  heading: string;
  sub: string;
  wide?: boolean;
  /** Where "See all" links to — omitted rows (genre/studio/network tile rows, where nothing is
   * actually truncated) render without the link at all, rather than pointing it somewhere wrong. */
  seeAllTo?: string;
  children: ReactNode;
}

function RowShell({ heading, sub, wide, seeAllTo, children }: RowShellProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);

  function scrollByPage(dir: 1 | -1) {
    const el = scrollerRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: "smooth" });
  }

  return (
    <section className="content-row">
      <div className="content-row-header">
        <h2>{heading}</h2>
        <span className="content-row-sub">{sub}</span>
        <div style={{ flex: 1 }} />
        <button type="button" className="btn btn-icon" onClick={() => scrollByPage(-1)} aria-label="Scroll left">
          <CaretLeft size={14} />
        </button>
        <button type="button" className="btn btn-icon" onClick={() => scrollByPage(1)} aria-label="Scroll right">
          <CaretRight size={14} />
        </button>
        {seeAllTo && (
          <Link to={seeAllTo} className="text-muted" style={{ fontSize: 12.5, marginLeft: 4 }}>
            See all
          </Link>
        )}
      </div>
      <div className={`poster-row k-scroll${wide ? " wide" : ""}`} ref={scrollerRef}>
        {children}
      </div>
    </section>
  );
}

function BecauseYouAddedRow() {
  const { data } = useApi(api.discoverBecauseYouAdded);
  const { stateFor, triggerAdd } = useAddToLibrary();

  if (!data || data.items.length === 0) {
    return null;
  }

  return (
    <RowShell heading={`Because You Added ${data.basedOnTitle}`} sub="from TMDB · similar titles">
      {data.items.map((item, i) => (
        <MediaCard
          key={item.mediaItemId ?? item.externalId}
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
    </RowShell>
  );
}

interface DiscoverRowProps {
  heading: string;
  sub: string;
  fetcher: () => Promise<DiscoverItem[]>;
  wide?: boolean;
  seeAllTo?: string;
}

function DiscoverRow({ heading, sub, fetcher, wide, seeAllTo }: DiscoverRowProps) {
  const { data, loading, error } = useApi(fetcher);
  const { stateFor, triggerAdd } = useAddToLibrary();

  return (
    <RowShell heading={heading} sub={sub} wide={wide} seeAllTo={seeAllTo}>
      {loading && <div className="text-muted" style={{ padding: "0 2px" }}>Loading…</div>}
      {error && (
        <div className="text-muted" style={{ padding: "0 2px" }}>
          Couldn't load — {error}
        </div>
      )}
      {data?.length === 0 && <div className="text-muted" style={{ padding: "0 2px" }}>Nothing here yet.</div>}
      {data?.map((item, i) => (
        <MediaCard
          key={item.mediaItemId ?? item.externalId}
          to={discoverItemLink(item)}
          title={item.title}
          year={item.year}
          posterPath={item.posterPath}
          backdropPath={item.backdropPath}
          wide={wide}
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
    </RowShell>
  );
}

interface GenreRowProps {
  heading: string;
  sub: string;
  fetcher: () => Promise<GenreTileType[]>;
  mediaType: "movie" | "tv";
}

function GenreRow({ heading, sub, fetcher, mediaType }: GenreRowProps) {
  const { data, loading, error } = useApi(fetcher);

  return (
    <RowShell heading={heading} sub={sub}>
      {loading && <div className="text-muted" style={{ padding: "0 2px" }}>Loading…</div>}
      {error && (
        <div className="text-muted" style={{ padding: "0 2px" }}>
          Couldn't load — {error}
        </div>
      )}
      {data?.map((genre, i) => (
        <Link
          key={genre.id}
          to={`/discover/genre/${mediaType}/${genre.id}`}
          state={{ name: genre.name }}
          className="genre-tile"
          style={{ background: tonalGradient(i) }}
        >
          {genre.name}
        </Link>
      ))}
    </RowShell>
  );
}

interface StudioRowProps {
  heading: string;
  sub: string;
  fetcher: () => Promise<StudioTileType[]>;
  kind: "studio" | "network";
}

function StudioRow({ heading, sub, fetcher, kind }: StudioRowProps) {
  const { data, loading, error } = useApi(fetcher);

  return (
    <RowShell heading={heading} sub={sub}>
      {loading && <div className="text-muted" style={{ padding: "0 2px" }}>Loading…</div>}
      {error && (
        <div className="text-muted" style={{ padding: "0 2px" }}>
          Couldn't load — {error}
        </div>
      )}
      {data?.map((studio) => (
        <Link
          key={studio.id}
          to={kind === "studio" ? `/discover/studio/${studio.id}` : `/discover/network/${studio.id}`}
          state={{ name: studio.name }}
          className="studio-tile"
        >
          <img src={logoUrl(studio.logoPath) ?? undefined} alt={studio.name} loading="lazy" />
        </Link>
      ))}
    </RowShell>
  );
}

function RecentRequestsRow() {
  const { data, loading, error } = useApi(api.listRequests);
  const recent = useMemo(
    () => (data ?? []).slice().sort((a, b) => b.requestedAt.localeCompare(a.requestedAt)).slice(0, 12),
    [data],
  );

  if (!loading && !error && recent.length === 0) return null;

  return (
    <RowShell heading="Recent Requests" sub="latest from your users" seeAllTo="/requests">
      {loading && <div className="text-muted" style={{ padding: "0 2px" }}>Loading…</div>}
      {error && (
        <div className="text-muted" style={{ padding: "0 2px" }}>
          Couldn't load — {error}
        </div>
      )}
      {recent.map((r, i) => (
        <MediaCard
          key={r.id}
          to={
            r.mediaItemId
              ? r.mediaType === "tv"
                ? `/shows/${r.mediaItemId}`
                : `/movies/${r.mediaItemId}`
              : `/search?q=${encodeURIComponent(r.title)}`
          }
          title={r.title}
          year={r.year}
          posterPath={r.posterPath}
          mediaItemId={r.mediaItemId}
          placeholderBackground={tonalGradient(i)}
        />
      ))}
    </RowShell>
  );
}

export default function HomePage() {
  return (
    <div>
      <Hero />

      <div className="page">
        <DiscoverRow
          heading="Recently Added to Your Library"
          sub="your latest additions"
          fetcher={api.discoverRecent}
          wide
          seeAllTo="/library"
        />
        <RecentRequestsRow />
        <DiscoverRow
          heading="Trending"
          sub="movies & series · from TMDB"
          fetcher={api.discoverTrending}
          seeAllTo="/discover/trending"
        />
        <DiscoverRow
          heading="Popular Movies"
          sub="all time · from TMDB"
          fetcher={api.discoverPopular}
          seeAllTo="/discover/list/popular"
        />
        <GenreRow heading="Movie Genres" sub="browse by genre" fetcher={api.discoverMovieGenres} mediaType="movie" />
        <DiscoverRow
          heading="Upcoming Movies"
          sub="from TMDB"
          fetcher={api.discoverUpcomingMovies}
          seeAllTo="/discover/list/upcoming-movies"
        />
        <StudioRow heading="Studios" sub="browse by studio" fetcher={api.discoverStudios} kind="studio" />
        <DiscoverRow
          heading="Popular Series"
          sub="all time · from TMDB"
          fetcher={api.discoverPopularTv}
          seeAllTo="/discover/list/popular-tv"
        />
        <GenreRow heading="Series Genres" sub="browse by genre" fetcher={api.discoverTvGenres} mediaType="tv" />
        <DiscoverRow
          heading="Upcoming Series"
          sub="from TMDB"
          fetcher={api.discoverUpcomingTv}
          seeAllTo="/discover/list/upcoming-tv"
        />
        <StudioRow heading="Networks" sub="browse by network" fetcher={api.discoverNetworks} kind="network" />
        <BecauseYouAddedRow />
      </div>
    </div>
  );
}
