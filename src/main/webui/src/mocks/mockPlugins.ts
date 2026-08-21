export interface PluginCapability {
  label: string;
}

export interface InstalledPlugin {
  id: string;
  name: string;
  version: string;
  publisher: "Kosmos core" | "Community";
  hue: "violet" | "cyan" | "amber" | "grey";
  description: string;
  capabilities: string[];
  enabled: boolean;
  statusKind: "good" | "warn" | "neutral";
  statusText: string;
  apiKeyConfigurable: boolean;
  /** Set only for plugins actually loaded through the real WASM host — see PluginsPage's
   *  fetch of GET /plugins. Everything else on this page is still design-phase sample data. */
  allowedHosts?: string[];
}

export interface RegistryPlugin {
  id: string;
  name: string;
  kind: string;
  publisher: "Kosmos core" | "Community";
  category: "Metadata" | "Artwork" | "Subtitles" | "Sync";
  description: string;
  comingSoon: boolean;
  reviewNote?: string;
  /** Set only for entries actually fetched from GET /plugins/registry (the real
   *  kosmos-plugin-registry repo) — installing one isn't wired up yet even so. */
  real?: boolean;
}

/**
 * Sample data for the marketplace UI — the install flow isn't wired to anything real yet.
 * TMDB and AniList themselves still run as hardcoded Java classes, not loaded plugins; PluginsPage
 * merges in real entries fetched from GET /plugins and GET /plugins/registry alongside this
 * sample data.
 */
export const installedPlugins: InstalledPlugin[] = [
  {
    id: "tmdb",
    name: "TMDB",
    version: "2.4.1",
    publisher: "Kosmos core",
    hue: "violet",
    description: "Movie and series metadata, artwork and cast credits from The Movie Database.",
    capabilities: ["Movies", "Series", "Artwork", "Cast"],
    enabled: true,
    statusKind: "good",
    statusText: "synced 12 min ago · 4,102 titles",
    apiKeyConfigurable: false,
  },
  {
    id: "anilist",
    name: "AniList",
    version: "1.9.0",
    publisher: "Kosmos core",
    hue: "cyan",
    description: "Anime metadata, absolute numbering and alternate titles from AniList.",
    capabilities: ["Anime", "Numbering", "Alt titles"],
    enabled: true,
    statusKind: "good",
    statusText: "synced 4 min ago · 196 titles",
    apiKeyConfigurable: false,
  },
  {
    id: "fanart-tv",
    name: "Fanart.tv",
    version: "1.2.6",
    publisher: "Community",
    hue: "violet",
    description: "High-resolution artwork and clean logos to layer over primary metadata.",
    capabilities: ["Artwork", "Logos"],
    enabled: true,
    statusKind: "good",
    statusText: "API key set · 812 assets cached",
    apiKeyConfigurable: true,
  },
  {
    id: "opensubtitles",
    name: "OpenSubtitles",
    version: "0.9.4",
    publisher: "Community",
    hue: "amber",
    description: "Subtitle search and download across a large community-maintained archive.",
    capabilities: ["Subtitles"],
    enabled: true,
    statusKind: "warn",
    statusText: "rate limited until 18:20",
    apiKeyConfigurable: true,
  },
];

export const registryPlugins: RegistryPlugin[] = [
  {
    id: "anidb",
    name: "AniDB",
    kind: "Metadata",
    publisher: "Community",
    category: "Metadata",
    description: "Hash-based anime identification and episode metadata from AniDB.",
    comingSoon: true,
    reviewNote: "pending review",
  },
  {
    id: "myanimelist",
    name: "MyAnimeList",
    kind: "Metadata",
    publisher: "Community",
    category: "Metadata",
    description: "Anime and manga metadata sourced from the MyAnimeList catalog.",
    comingSoon: true,
    reviewNote: "in progress",
  },
  {
    id: "trakt",
    name: "Trakt",
    kind: "Sync",
    publisher: "Kosmos core",
    category: "Sync",
    description: "Sync watch history and ratings with your Trakt account.",
    comingSoon: false,
  },
  {
    id: "thetvdb",
    name: "TheTVDB",
    kind: "Metadata",
    publisher: "Community",
    category: "Metadata",
    description: "Series and episode metadata with strong TV numbering support.",
    comingSoon: false,
  },
  {
    id: "subdl",
    name: "Subdl",
    kind: "Subtitles",
    publisher: "Community",
    category: "Subtitles",
    description: "Additional subtitle source with strong non-English coverage.",
    comingSoon: false,
  },
  {
    id: "kometa",
    name: "Poster Sets (Kometa)",
    kind: "Artwork",
    publisher: "Community",
    category: "Artwork",
    description: "Curated matching poster sets sourced from the Kometa community collections.",
    comingSoon: false,
  },
];
