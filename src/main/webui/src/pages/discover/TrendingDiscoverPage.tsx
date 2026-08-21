import { useState } from "react";
import { api } from "../../api/client";
import { DiscoverGrid } from "../../components/DiscoverGrid";
import { LanguageFilterDropdown } from "../../components/LanguageFilterDropdown";

type TimeWindow = "day" | "week";
type MediaType = "all" | "movie" | "tv";

const WINDOWS: { value: TimeWindow; label: string }[] = [
  { value: "day", label: "Daily" },
  { value: "week", label: "Weekly" },
];

const MEDIA_TYPES: { value: MediaType; label: string }[] = [
  { value: "all", label: "All" },
  { value: "movie", label: "Movies" },
  { value: "tv", label: "Series" },
];

/** "See all" for Discover/Home's Trending row — its own page since, unlike every other list, it
 * has real window (day/week) and type (all/movie/tv) filters on top of the language filter every
 * list page shares, matching Jellyseerr's own. */
export default function TrendingDiscoverPage() {
  const [timeWindow, setTimeWindow] = useState<TimeWindow>("week");
  const [mediaType, setMediaType] = useState<MediaType>("all");
  const [hiddenLanguages, setHiddenLanguages] = useState<Set<string>>(new Set());

  const excludeLanguages = Array.from(hiddenLanguages);

  return (
    <DiscoverGrid
      title="Trending"
      depKey={`${timeWindow}-${mediaType}-${excludeLanguages.join(",")}`}
      fetcher={(page) => api.discoverTrending(timeWindow, mediaType, page, excludeLanguages)}
      filters={
        <>
          <div className="seg">
            {MEDIA_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                className={mediaType === t.value ? "active" : ""}
                onClick={() => setMediaType(t.value)}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div className="seg">
            {WINDOWS.map((w) => (
              <button
                key={w.value}
                type="button"
                className={timeWindow === w.value ? "active" : ""}
                onClick={() => setTimeWindow(w.value)}
              >
                {w.label}
              </button>
            ))}
          </div>
          <LanguageFilterDropdown hidden={hiddenLanguages} onChange={setHiddenLanguages} />
        </>
      }
    />
  );
}
