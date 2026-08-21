export interface Movie {
  id: string;
  title: string;
  year: number | null;
  runtimeMinutes: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  addedAt: string;
  qualityProfileId: string | null;
}

export interface Show {
  id: string;
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  status: string | null;
  addedAt: string;
  qualityProfileId: string | null;
}

export type EpisodeStatus = "MISSING" | "GRABBED" | "IMPORTED" | "AVAILABLE";

export interface Episode {
  id: string;
  episodeNumber: number;
  title: string;
  overview: string | null;
  airDate: string | null;
  runtimeMinutes: number | null;
  stillPath: string | null;
  status: EpisodeStatus;
}

export interface EpisodeDetail {
  id: string;
  episodeNumber: number;
  title: string;
  runtimeMinutes: number | null;
  showId: string;
  showTitle: string;
  seasonNumber: number;
  qualityProfileId: string | null;
  status: EpisodeStatus;
}

export interface Season {
  id: string;
  seasonNumber: number;
  name: string;
  overview: string | null;
  posterPath: string | null;
  episodeCount: number | null;
  episodes: Episode[];
}

export interface ShowDetail extends Show {
  seasons: Season[];
}

export interface Anime {
  id: string;
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  status: string | null;
  episodeCountTotal: number | null;
  addedAt: string;
  qualityProfileId: string | null;
}

export interface AnimeEpisode {
  id: string;
  episodeNumber: number | null;
  absoluteEpisodeNumber: number | null;
  episodeType: string;
  title: string;
  overview: string | null;
  airDate: string | null;
  runtimeMinutes: number | null;
  stillPath: string | null;
  status: EpisodeStatus;
}

export interface AnimeDetail extends Anime {
  episodes: AnimeEpisode[];
}

export interface AnimeEpisodeDetail {
  id: string;
  episodeNumber: number | null;
  absoluteEpisodeNumber: number | null;
  title: string;
  runtimeMinutes: number | null;
  animeId: string;
  animeTitle: string;
  qualityProfileId: string | null;
  status: EpisodeStatus;
}

export interface PluginManifest {
  slug: string;
  name: string;
  version: string;
  kind: string;
  entryPoint: string;
  permissions: {
    allowedHosts: string[];
  };
}

export interface RegistryEntry {
  slug: string;
  name: string;
  description: string;
  category: "Metadata" | "Artwork" | "Subtitles" | "Sync";
  publisher: string;
  repository: string;
  version: string;
  checksum: string;
  homepage: string | null;
}

export interface MetadataSearchResult {
  mediaItemId: string | null;
  externalId: string;
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  voteAverage: number | null;
  mediaType: "movie" | "tv" | "anime";
  inLibrary: boolean;
  partiallyAvailable: boolean;
}

export interface DetailFact {
  k: string;
  v: string;
}

export interface CastMember {
  name: string;
  role: string;
  profilePath: string | null;
}

export interface MediaDetailExtras {
  genres: string[];
  facts: DetailFact[];
  voteAverage: number | null;
  voteCount: number | null;
  certification: string | null;
  cast: CastMember[];
  similar: MetadataSearchResult[];
}

export interface PreviewEpisode {
  episodeNumber: number;
  title: string;
  airDate: string | null;
}

export interface PreviewSeason {
  seasonNumber: number;
  name: string;
  episodeCount: number | null;
  episodes: PreviewEpisode[];
}

/** The detail screen for a title Kosmos doesn't own yet — see {@link MediaDetailExtras}. */
export interface MediaPreview {
  externalId: string;
  pluginSlug: string;
  mediaType: "movie" | "tv" | "anime";
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  genres: string[];
  facts: DetailFact[];
  voteAverage: number | null;
  voteCount: number | null;
  certification: string | null;
  cast: CastMember[];
  similar: MetadataSearchResult[];
  seasons: PreviewSeason[];
  episodes: PreviewEpisode[];
}

export interface Indexer {
  id: string;
  name: string;
  baseUrl: string;
  apiKeySet: boolean;
  enabled: boolean;
  createdAt: string;
}

export interface TorznabResult {
  title: string;
  downloadUrl: string;
  sizeBytes: number;
  seeders: number | null;
  peers: number | null;
  publishedAt: string | null;
}

export interface ParsedRelease {
  title: string;
  year: number | null;
  resolution: string | null;
  source: string | null;
  videoCodec: string | null;
  audioCodec: string | null;
  edition: string | null;
  releaseGroup: string | null;
  proper: boolean;
  repack: boolean;
}

export interface CustomFormatMatch {
  customFormatId: string;
  name: string;
  score: number;
  matched: boolean;
}

export interface ScoredSearchResult {
  raw: TorznabResult;
  parsed: ParsedRelease;
  score: number | null;
  cutoffScore: number | null;
  passesCutoff: boolean | null;
  formatBreakdown: CustomFormatMatch[] | null;
  sizeGateReason: string | null;
}

export interface QualityDefinition {
  id: string;
  resolution: string;
  source: string;
  minMbPerMinute: number;
  maxMbPerMinute: number;
}

export interface CustomFormat {
  id: string;
  name: string;
  score: number;
  rule: string;
  trashId: string | null;
}

export interface TrashImportResult {
  created: number;
  updated: number;
  skipped: string[];
}

export interface QualityProfile {
  id: string;
  name: string;
  cutoffScore: number;
  customFormats: CustomFormat[];
}

export interface DownloadClient {
  id: string;
  name: string;
  type: string;
  baseUrl: string;
  username: string | null;
  passwordSet: boolean;
  category: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface Grab {
  id: string;
  releaseId: string;
  title: string;
  downloadClientId: string;
  downloadClientName: string;
  status: string;
  grabbedAt: string;
}

export interface LibraryFile {
  id: string;
  mediaItemId: string;
  path: string;
  sizeBytes: number;
  matchMethod: string;
  importedAt: string;
  verified: boolean;
  container: string | null;
  videoCodec: string | null;
  resolutionWidth: number | null;
  resolutionHeight: number | null;
  durationSeconds: number | null;
  hdrFormat: string | null;
  bitDepth: number | null;
}

export interface Notifier {
  id: string;
  name: string;
  type: string;
  urlSet: boolean;
  tokenSet: boolean;
  target: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface JellyfinServer {
  id: string;
  name: string;
  baseUrl: string;
  apiKeySet: boolean;
  enabled: boolean;
  createdAt: string;
}

export interface JellyfinLibrary {
  id: string;
  name: string;
  collectionType: string | null;
  locations: string[];
}

export interface SetupStatus {
  needsSetup: boolean;
}

export interface JellyfinSyncResult {
  scanned: number;
  linked: number;
  created: number;
  skippedNoTmdbId: number;
  alreadySynced: number;
  showsScanned: number;
  showsLinked: number;
  showsCreated: number;
  showsSkippedNoTmdbId: number;
  showsAlreadySynced: number;
  episodeFilesLinked: number;
  usersCreated: number;
  usersUpdated: number;
}

export interface ScheduledJob {
  id: string;
  name: string;
  intervalSeconds: number;
  enabled: boolean;
  lastRunAt: string | null;
  lastStatus: string | null;
  lastMessage: string | null;
}

export interface MetadataStatus {
  tmdbConfigured: boolean;
}

export interface TmdbTestResult {
  ok: boolean;
}

export interface LibraryStats {
  movieCount: number;
  seriesCount: number;
  animeCount: number;
  usedBytes: number;
  totalBytes: number | null;
}

export type LibraryContentType = "movie" | "show" | "anime";

export interface LibraryRootFolder {
  id: string;
  path: string;
  contentTypes: LibraryContentType[];
  createdAt: string;
}

export interface DirectoryEntry {
  name: string;
  path: string;
}

export interface BrowseResult {
  path: string;
  parentPath: string | null;
  directories: DirectoryEntry[];
}

export interface TestIndexerResult {
  ok: boolean;
  message: string;
}

export interface TestDownloadClientResult {
  ok: boolean;
  message: string;
}

export interface ImportFromProwlarrResult {
  imported: number;
  skippedDisabled: number;
}

export interface DiscoverItem {
  mediaItemId: string | null;
  externalId: string | null;
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  voteAverage: number | null;
  mediaType: "movie" | "tv";
  inLibrary: boolean;
  partiallyAvailable: boolean;
}

export interface GenreTile {
  id: number;
  name: string;
}

export interface BecauseYouAddedResult {
  basedOnTitle: string;
  items: DiscoverItem[];
}

export interface StudioTile {
  id: number;
  name: string;
  logoPath: string;
}

export type RequestStatus = "PENDING" | "APPROVED" | "AVAILABLE" | "DECLINED";

export interface MediaRequest {
  id: string;
  requestedByDisplayName: string;
  mine: boolean;
  mediaType: "movie" | "tv";
  externalId: string;
  title: string;
  year: number | null;
  overview: string | null;
  posterPath: string | null;
  backdropPath: string | null;
  qualityProfileId: string | null;
  qualityProfileName: string | null;
  status: RequestStatus;
  note: string | null;
  mediaItemId: string | null;
  requestedAt: string;
  decidedAt: string | null;
}

export interface User {
  id: string;
  username: string;
  displayName: string;
  role: "ADMIN" | "USER";
  jellyfinLinked: boolean;
  createdAt: string;
}
