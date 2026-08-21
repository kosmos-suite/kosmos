export interface TrendingSearchItem {
  id: string;
  title: string;
  year: number;
  kind: "Movie" | "Series" | "Anime";
  rank: number;
  tone: number;
}

/** No search-history or trending-search endpoint exists yet. Shown on Search's idle (no-query) state. */
export const recentSearches: string[] = ["frieren", "villeneuve", "satoshi kon", "2160p remux"];

export const trendingSearches: TrendingSearchItem[] = [
  { id: "t1", title: "Dune: Part Two", year: 2024, kind: "Movie", rank: 1, tone: 0 },
  { id: "t2", title: "Shogun", year: 2024, kind: "Series", rank: 2, tone: 1 },
  { id: "t3", title: "Frieren: Beyond Journey's End", year: 2023, kind: "Anime", rank: 3, tone: 2 },
  { id: "t4", title: "The Substance", year: 2024, kind: "Movie", rank: 4, tone: 3 },
  { id: "t5", title: "Blue Eye Samurai", year: 2023, kind: "Series", rank: 5, tone: 4 },
  { id: "t6", title: "Delicious in Dungeon", year: 2024, kind: "Anime", rank: 6, tone: 5 },
];
