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
import { backdropUrl } from "../api/tmdbImage";
import type { DiscoverItem } from "../api/types";
import { MovieCard } from "../components/MovieCard";
import { useApi } from "../hooks/useApi";
import { becauseYouAdded, type DiscoverMovie } from "../mocks/mockLibrary";
import { tonalGradient } from "../utils/tonalGradient";

function discoverItemLink(item: DiscoverItem): string {
  if (item.mediaItemId) {
    return item.mediaType === "tv" ? `/shows/${item.mediaItemId}` : `/movies/${item.mediaItemId}`;
  }
  return `/search?q=${encodeURIComponent(item.title)}`;
}

const HERO_SLIDE_COUNT = 5;
const HERO_INTERVAL_MS = 7600;

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
        {slides.map((s, i) => {
          const art = backdropUrl(s.backdropPath, "original");
          return (
            <div key={s.mediaItemId ?? s.externalId} className={`hero-slide${i === active ? " active" : ""}`}>
              <div
                className="hero-slide-art"
                style={
                  art
                    ? { backgroundImage: `url(${art})`, backgroundSize: "cover", backgroundPosition: "center" }
                    : { background: tonalGradient(i) }
                }
              />
            </div>
          );
        })}
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
  children: ReactNode;
}

function RowShell({ heading, sub, wide, children }: RowShellProps) {
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
        <Link to="/library" className="text-muted" style={{ fontSize: 12.5, marginLeft: 4 }}>
          See all
        </Link>
      </div>
      <div className={`poster-row k-scroll${wide ? " wide" : ""}`} ref={scrollerRef}>
        {children}
      </div>
    </section>
  );
}

interface MockContentRowProps {
  heading: string;
  sub: string;
  movies: DiscoverMovie[];
}

function MockContentRow({ heading, sub, movies }: MockContentRowProps) {
  return (
    <RowShell heading={heading} sub={sub}>
      {movies.map((movie, i) => (
        <MovieCard
          key={movie.id}
          to="/library"
          title={movie.title}
          year={movie.year}
          posterPath={null}
          status={movie.status}
          progress={movie.progress}
          placeholderBackground={tonalGradient(i)}
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
}

function DiscoverRow({ heading, sub, fetcher, wide }: DiscoverRowProps) {
  const { data, loading, error } = useApi(fetcher);

  return (
    <RowShell heading={heading} sub={sub} wide={wide}>
      {loading && <div className="text-muted" style={{ padding: "0 2px" }}>Loading…</div>}
      {error && (
        <div className="text-muted" style={{ padding: "0 2px" }}>
          Couldn't load — {error}
        </div>
      )}
      {data?.length === 0 && <div className="text-muted" style={{ padding: "0 2px" }}>Nothing here yet.</div>}
      {data?.map((item, i) => (
        <MovieCard
          key={item.mediaItemId ?? item.externalId}
          to={discoverItemLink(item)}
          title={item.title}
          year={item.year}
          posterPath={item.posterPath}
          status={item.inLibrary ? "in-library" : undefined}
          placeholderBackground={tonalGradient(i)}
        />
      ))}
    </RowShell>
  );
}

export default function HomePage() {
  return (
    <div>
      {/* Note: "Because You Added" still uses mock data — no recommendation engine exists yet. */}
      <Hero />

      <div className="page">
        <DiscoverRow heading="Trending This Week" sub="from TMDB · updated 12h" fetcher={api.discoverTrending} />
        <DiscoverRow heading="Popular Movies" sub="all time · from TMDB" fetcher={api.discoverPopular} />
        <DiscoverRow
          heading="Recently Added to Your Library"
          sub="your latest additions"
          fetcher={api.discoverRecent}
          wide
        />
        <MockContentRow heading="Because You Added Dune: Part Two" sub="similar sci-fi picks" movies={becauseYouAdded} />
      </div>
    </div>
  );
}
