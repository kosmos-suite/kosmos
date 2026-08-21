/**
 * Supplementary movie-detail content with no backend source yet — TMDB credits/similar-titles
 * aren't fetched anywhere (only search results are). Applied to every movie detail page
 * regardless of the real movie, since there is nowhere yet to look up per-movie cast/similar-title
 * data.
 */

export type LibraryStatus = "not-in-library" | "in-library" | "missing";

export interface CastMember {
  name: string;
  role: string;
  initials: string;
}

export type SimilarStatus = "library" | "missing" | "new";

export interface SimilarTitle {
  title: string;
  meta: string;
  status: SimilarStatus;
}

export interface DetailFact {
  k: string;
  v: string;
}

export const cast: CastMember[] = [
  { name: "Timothée Chalamet", role: "Paul Atreides", initials: "TC" },
  { name: "Zendaya", role: "Chani", initials: "Z" },
  { name: "Rebecca Ferguson", role: "Lady Jessica", initials: "RF" },
  { name: "Austin Butler", role: "Feyd-Rautha", initials: "AB" },
  { name: "Florence Pugh", role: "Princess Irulan", initials: "FP" },
  { name: "Javier Bardem", role: "Stilgar", initials: "JB" },
  { name: "Josh Brolin", role: "Gurney Halleck", initials: "JBr" },
  { name: "Christopher Walken", role: "Emperor Shaddam IV", initials: "CW" },
  { name: "Léa Seydoux", role: "Lady Margot", initials: "LS" },
];

export const similarTitles: SimilarTitle[] = [
  { title: "Dune", meta: "2021 · Movie", status: "library" },
  { title: "Blade Runner 2049", meta: "2017 · Movie", status: "library" },
  { title: "Arrival", meta: "2016 · Movie", status: "library" },
  { title: "Foundation S2", meta: "2023 · Series", status: "new" },
  { title: "Children of Dune", meta: "2003 · Mini-series", status: "missing" },
  { title: "Solaris", meta: "1972 · Movie", status: "new" },
  { title: "Stalker", meta: "1979 · Movie", status: "new" },
  { title: "Annihilation", meta: "2018 · Movie", status: "library" },
  { title: "Alita: Battle Angel", meta: "2019 · Movie", status: "new" },
];

export const genres = ["Science Fiction", "Adventure", "Drama", "Epic"];

export const director = "Denis Villeneuve";
export const studio = "Legendary Pictures";
export const certification = "PG-13";
export const tmdbRating = 8.6;
export const tmdbVotes = "6,204 votes";
export const releaseDateLabel = "1 Mar 2024";
export const language = "English";
export const collection = "Dune (3 titles)";
export const runtimeLabel = "2h 46m";

export const detailFacts: DetailFact[] = [
  { k: "Director", v: director },
  { k: "Studio", v: studio },
  { k: "Release", v: releaseDateLabel },
  { k: "Language", v: language },
  { k: "TMDB", v: `${tmdbRating} · ${tmdbVotes}` },
  { k: "Collection", v: collection },
];

export const fileFacts: DetailFact[] = [
  { k: "Quality", v: "1080p BluRay x264" },
  { k: "Size", v: "8.42 GB" },
  { k: "Added", v: "14 Mar 2026" },
  { k: "Audio / Subs", v: "EN 5.1 · EN, FR" },
];

export const filePath = "/media/movies/{title} ({year})/{title}.{year}.1080p.BluRay.x264-KOSMOS.mkv";

export const missingFacts: DetailFact[] = [
  { k: "Quality profile", v: "HD-2160p" },
  { k: "Last search", v: "4 hours ago" },
  { k: "Releases seen", v: "3 · all rejected" },
];
