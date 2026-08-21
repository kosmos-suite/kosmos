import type { MovieStatus } from "../components/MovieCard";

/**
 * Sample data for Discover rows + hero carousel — there's no /discover or /trending endpoint yet.
 * LibraryPage uses the real /movies API instead — see LibraryPage.tsx for how it fills in the
 * quality/size/monitoring fields Movie doesn't have yet.
 */

export interface DiscoverMovie {
  id: string;
  title: string;
  year: number;
  status?: MovieStatus;
  /** Percent, 0-100. Only rendered when present. */
  progress?: number;
}

export const trendingThisWeek: DiscoverMovie[] = [
  { id: "m1", title: "Dune: Part Two", year: 2024 },
  { id: "m2", title: "The Substance", year: 2024, status: "downloading", progress: 41 },
  { id: "m3", title: "Anora", year: 2024, status: "in-library" },
  { id: "m4", title: "Frieren: Beyond Journey's End", year: 2023, status: "in-library" },
  { id: "m5", title: "Nosferatu", year: 2024, status: "missing" },
  { id: "m6", title: "Challengers", year: 2024 },
  { id: "m7", title: "Flow", year: 2024, status: "in-library" },
  { id: "m8", title: "Conclave", year: 2024, status: "in-library" },
  { id: "m9", title: "The Wild Robot", year: 2024, status: "downloading", progress: 78 },
];

export const popularMovies: DiscoverMovie[] = [
  { id: "m10", title: "Blade Runner 2049", year: 2017, status: "in-library" },
  { id: "m11", title: "Arrival", year: 2016, status: "in-library" },
  { id: "m12", title: "Interstellar", year: 2014, status: "in-library" },
  { id: "m13", title: "Parasite", year: 2019, status: "missing" },
  { id: "m14", title: "Whiplash", year: 2014 },
  { id: "m15", title: "Sicario", year: 2015, status: "in-library" },
  { id: "m16", title: "Oppenheimer", year: 2023, status: "in-library" },
  { id: "m17", title: "Poor Things", year: 2023, status: "missing" },
  { id: "m18", title: "Everything Everywhere All at Once", year: 2022, status: "in-library" },
];

export const recentlyAdded: DiscoverMovie[] = [
  { id: "m19", title: "Perfect Blue", year: 1997, status: "in-library" },
  { id: "m20", title: "Cowboy Bebop", year: 1998, status: "downloading", progress: 62 },
  { id: "m21", title: "Millennium Actress", year: 2001, status: "in-library" },
  { id: "m22", title: "Paprika", year: 2006, status: "in-library" },
  { id: "m23", title: "Ghost in the Shell", year: 1995, status: "missing" },
  { id: "m24", title: "The Brutalist", year: 2024, status: "in-library" },
  { id: "m25", title: "Shogun", year: 2024, status: "in-library" },
];

export const becauseYouAdded: DiscoverMovie[] = [
  { id: "m26", title: "Dune: Part One", year: 2021, status: "in-library" },
  { id: "m27", title: "Blade Runner 2049", year: 2017, status: "in-library" },
  { id: "m28", title: "Arrival", year: 2016, status: "in-library" },
  { id: "m29", title: "Sicario", year: 2015, status: "in-library" },
  { id: "m30", title: "Prisoners", year: 2013, status: "missing" },
  { id: "m31", title: "Enemy", year: 2013 },
  { id: "m32", title: "The Wild Robot", year: 2024, status: "downloading", progress: 78 },
  { id: "m33", title: "Interstellar", year: 2014, status: "in-library" },
  { id: "m34", title: "Annihilation", year: 2018, status: "missing" },
];

