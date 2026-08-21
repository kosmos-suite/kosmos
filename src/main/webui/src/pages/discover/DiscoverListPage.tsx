import { useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../../api/client";
import type { DiscoverItem } from "../../api/types";
import { DiscoverGrid } from "../../components/DiscoverGrid";
import { LanguageFilterDropdown } from "../../components/LanguageFilterDropdown";

const LISTS: Record<
  string,
  { title: string; fetcher: (page: number, excludeLanguages: string[]) => Promise<DiscoverItem[]> }
> = {
  popular: { title: "Popular Movies", fetcher: api.discoverPopular },
  "upcoming-movies": { title: "Upcoming Movies", fetcher: api.discoverUpcomingMovies },
  "popular-tv": { title: "Popular Series", fetcher: api.discoverPopularTv },
  "upcoming-tv": { title: "Upcoming Series", fetcher: api.discoverUpcomingTv },
};

/** "See all" click-through for a fixed-list Discover/Home row (Popular, Upcoming…) — Trending has
 * its own dedicated page (window/type filters too), genre/studio/network rows their own (keyed by
 * id) — all sharing this same language filter. */
export default function DiscoverListPage() {
  const { kind } = useParams<{ kind: string }>();
  const entry = kind ? LISTS[kind] : undefined;
  const [hiddenLanguages, setHiddenLanguages] = useState<Set<string>>(new Set());

  if (!entry) {
    return <div className="page text-muted">Unknown list.</div>;
  }

  const excludeLanguages = Array.from(hiddenLanguages);

  return (
    <DiscoverGrid
      title={entry.title}
      depKey={`${kind}-${excludeLanguages.join(",")}`}
      fetcher={(page) => entry.fetcher(page, excludeLanguages)}
      filters={<LanguageFilterDropdown hidden={hiddenLanguages} onChange={setHiddenLanguages} />}
    />
  );
}
