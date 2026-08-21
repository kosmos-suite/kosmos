import { CaretLeftIcon as CaretLeft, CaretRightIcon as CaretRight } from "@phosphor-icons/react";
import { useRef } from "react";
import { profileUrl } from "../api/tmdbImage";
import type { CastMember, SimilarTitle } from "../api/types";
import { tonalGradient } from "../utils/tonalGradient";
import { MediaCard } from "./MediaCard";

export function CastRow({ cast }: { cast: CastMember[] }) {
  if (cast.length === 0) return null;
  return (
    <div className="content-row">
      <div className="content-row-header">
        <h2>Cast</h2>
        <span className="content-row-sub">
          top billed · {cast.length} credited
        </span>
      </div>
      <div className="cast-row k-scroll">
        {cast.map((p, i) => {
          const photo = profileUrl(p.profilePath);
          return (
            <div key={p.name + i} className="cast-item">
              <div className="cast-avatar">
                {photo ? <img src={photo} alt="" loading="lazy" /> : <span>{initials(p.name)}</span>}
              </div>
              <div className="cast-name">{p.name}</div>
              <div className="cast-role">{p.role}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/**
 * No library-status cross-reference (see {@code MediaDetailExtras}'s own doc comment) — cards link
 * out to a search for the title rather than showing an in-library check or hover-add icon.
 */
export function SimilarRow({ items }: { items: SimilarTitle[] }) {
  const rowRef = useRef<HTMLDivElement>(null);
  if (items.length === 0) return null;

  function scroll(dir: 1 | -1) {
    const el = rowRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: "smooth" });
  }

  return (
    <div className="content-row" style={{ paddingBottom: 40 }}>
      <div className="content-row-header">
        <h2>More Like This</h2>
        <span className="content-row-sub">TMDB similar · {items.length} titles</span>
        <div style={{ flex: 1 }} />
        <button type="button" className="row-scroll-btn" onClick={() => scroll(-1)}>
          <CaretLeft size={15} />
        </button>
        <button type="button" className="row-scroll-btn" onClick={() => scroll(1)}>
          <CaretRight size={15} />
        </button>
      </div>
      <div className="poster-row k-scroll" ref={rowRef}>
        {items.map((s, i) => (
          <MediaCard
            key={s.externalId}
            to={`/search?q=${encodeURIComponent(s.title)}`}
            title={s.title}
            year={s.year}
            posterPath={s.posterPath}
            mediaType={s.mediaType}
            placeholderBackground={tonalGradient(i)}
          />
        ))}
      </div>
    </div>
  );
}

function initials(name: string): string {
  return name
    .split(" ")
    .map((w) => w[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
}
