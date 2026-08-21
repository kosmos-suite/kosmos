package de.oppahansi.kosmos.metadata.tmdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.metadata.MetadataProvider;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Searches movies and TV shows via the TMDB v3 API. */
@ApplicationScoped
public class TmdbMetadataProvider implements MetadataProvider {

  private static final String SEARCH_URL = "https://api.themoviedb.org/3/search/movie";
  private static final String TV_SEARCH_URL = "https://api.themoviedb.org/3/search/tv";
  private static final String MOVIE_URL = "https://api.themoviedb.org/3/movie/";
  private static final String TV_URL = "https://api.themoviedb.org/3/tv/";
  private static final String TRENDING_MOVIE_URL = "https://api.themoviedb.org/3/trending/movie/";
  private static final String TRENDING_TV_URL = "https://api.themoviedb.org/3/trending/tv/";
  private static final String TRENDING_ALL_URL = "https://api.themoviedb.org/3/trending/all/";
  private static final String POPULAR_URL = "https://api.themoviedb.org/3/movie/popular";
  private static final String UPCOMING_MOVIES_URL = "https://api.themoviedb.org/3/movie/upcoming";
  private static final String POPULAR_TV_URL = "https://api.themoviedb.org/3/tv/popular";
  private static final String DISCOVER_MOVIE_URL = "https://api.themoviedb.org/3/discover/movie";
  private static final String DISCOVER_TV_URL = "https://api.themoviedb.org/3/discover/tv";
  private static final String MOVIE_GENRES_URL = "https://api.themoviedb.org/3/genre/movie/list";
  private static final String TV_GENRES_URL = "https://api.themoviedb.org/3/genre/tv/list";
  private static final String AUTH_URL = "https://api.themoviedb.org/3/authentication";

  @ConfigProperty(name = "kosmos.metadata.tmdb.api-key")
  Optional<String> apiKey;

  @Inject TmdbTvDetailClient tmdbTvDetailClient;

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String pluginSlug() {
    return "tmdb";
  }

  /** Whether the API key is set — a deploy-time env var, not something the UI can save. */
  public boolean isConfigured() {
    return apiKey.isPresent();
  }

  /**
   * Verifies the configured key actually works, via TMDB's own dedicated key-check endpoint ({@code
   * /authentication}, {"success":true} for a valid key) rather than inferring it from a real
   * search, which would conflate "key is bad" with "no results for this query".
   */
  public boolean testConnection() {
    if (apiKey.isEmpty()) {
      return false;
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(AUTH_URL + "?api_key=" + apiKey.get()))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200
          && objectMapper.readTree(response.body()).path("success").asBoolean(false);
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  /**
   * Search-as-you-type in SearchPage fires this on every debounced keystroke — with TV search
   * (below) now firing alongside it, this is the one real repeated-call hot path against TMDB (see
   * the research dossier's "HTTP response caching" entry). 24h keeps duplicate/near-duplicate
   * typing traffic from re-hitting TMDB within a session while staying well under TMDB's own
   * 6-month cache-retention ceiling; the bounded max-size (see application.properties) keeps
   * arbitrary user-typed queries from growing the cache without limit.
   */
  @Override
  @CacheResult(cacheName = "tmdb-search-movie")
  public List<MetadataSearchResult> search(String query) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(buildSearchUrl(query))).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return parse(response.body());
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("TMDB search failed", e);
    }
  }

  /**
   * Backs Discover/Home's "Trending" row/hero and the "Trending" list page's "Movies" filter —
   * TMDB's own {@code /trending/movie/{window}}, {@code window} being {@code day} or {@code week}.
   */
  @CacheResult(cacheName = "tmdb-trending")
  public List<MetadataSearchResult> fetchTrendingMovies(
      String window, int page, String excludeLanguages) {
    return fetchMoviesFiltered(
        TRENDING_MOVIE_URL + window,
        "page=" + page,
        excludeLanguages,
        "TMDB trending fetch failed");
  }

  /**
   * Backs the "Trending" list page's "Series" filter — same idea as {@link #fetchTrendingMovies}.
   */
  @CacheResult(cacheName = "tmdb-trending-tv")
  public List<MetadataSearchResult> fetchTrendingTv(
      String window, int page, String excludeLanguages) {
    return fetchTvFiltered(
        TRENDING_TV_URL + window,
        "page=" + page,
        excludeLanguages,
        "TMDB TV trending fetch failed");
  }

  /**
   * Backs Discover/Home's "Trending" row/hero (default) and the "Trending" list page's "All" filter
   * — TMDB's own {@code /trending/all/{window}}, mixing movies and series in one popularity-sorted
   * list with a {@code media_type} per result. Unlike every other list here, this can't be bound to
   * a single typed DTO (a movie result has {@code title}, a TV result has {@code name}, and the
   * list also contains person results this filters out entirely) — parsed by hand via {@link
   * #parseTrendingAll}.
   */
  @CacheResult(cacheName = "tmdb-trending-all")
  public List<MetadataSearchResult> fetchTrendingAll(
      String window, int page, String excludeLanguages) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      String url = TRENDING_ALL_URL + window + "?api_key=" + apiKey.orElseThrow() + "&page=" + page;
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return parseTrendingAll(response.body(), parseLanguages(excludeLanguages));
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("TMDB trending fetch failed", e);
    }
  }

  /**
   * Parses a comma-separated ISO 639-1 list (e.g. {@code "zh,hi"}) — blank/null means "exclude
   * nothing", so callers can filter with {@code !excluded.contains(lang)} unconditionally.
   */
  private Set<String> parseLanguages(String csv) {
    if (csv == null || csv.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private List<MetadataSearchResult> parseTrendingAll(String json, Set<String> excludeLanguages) {
    try {
      List<MetadataSearchResult> out = new ArrayList<>();
      for (JsonNode r : objectMapper.readTree(json).path("results")) {
        String mediaType = r.path("media_type").asText("");
        String language = r.path("original_language").asText("");
        if (excludeLanguages.contains(language)) {
          continue;
        }
        if ("movie".equals(mediaType)) {
          out.add(
              new MetadataSearchResult(
                  String.valueOf(r.path("id").asInt()),
                  textOrNull(r, "title"),
                  extractYear(textOrNull(r, "release_date")),
                  textOrNull(r, "overview"),
                  textOrNull(r, "poster_path"),
                  textOrNull(r, "backdrop_path"),
                  r.path("vote_average").isNumber() ? r.path("vote_average").asDouble() : null,
                  "movie"));
        } else if ("tv".equals(mediaType)) {
          out.add(
              new MetadataSearchResult(
                  String.valueOf(r.path("id").asInt()),
                  textOrNull(r, "name"),
                  extractYear(textOrNull(r, "first_air_date")),
                  textOrNull(r, "overview"),
                  textOrNull(r, "poster_path"),
                  textOrNull(r, "backdrop_path"),
                  null,
                  "tv"));
        }
        // "person" results and anything else are skipped — not a title Kosmos can show a card for.
      }
      return out;
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid TMDB response", e);
    }
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }

  /**
   * Backs Discover/Home's "Popular Movies" row (page 1, unfiltered) and the "Popular Movies" list
   * page's infinite scroll and language filter.
   */
  @CacheResult(cacheName = "tmdb-popular")
  public List<MetadataSearchResult> fetchPopularMovies(int page, String excludeLanguages) {
    return fetchMoviesFiltered(
        POPULAR_URL, "page=" + page, excludeLanguages, "TMDB popular fetch failed");
  }

  /**
   * Backs Discover/Home's "Upcoming Movies" row (page 1, unfiltered) and its list page's infinite
   * scroll and language filter — TMDB's own {@code /movie/upcoming}.
   */
  @CacheResult(cacheName = "tmdb-upcoming-movies")
  public List<MetadataSearchResult> fetchUpcomingMovies(int page, String excludeLanguages) {
    return fetchMoviesFiltered(
        UPCOMING_MOVIES_URL, "page=" + page, excludeLanguages, "TMDB upcoming movies fetch failed");
  }

  /**
   * Backs Discover/Home's "Popular Series" row (page 1, unfiltered) and its list page's infinite
   * scroll and language filter.
   */
  @CacheResult(cacheName = "tmdb-popular-tv")
  public List<MetadataSearchResult> fetchPopularTv(int page, String excludeLanguages) {
    return fetchTvFiltered(
        POPULAR_TV_URL, "page=" + page, excludeLanguages, "TMDB popular TV fetch failed");
  }

  private static final int UPCOMING_TV_TARGET_COUNT = 20;
  private static final int UPCOMING_TV_MAX_PAGES = 5;

  /**
   * Backs Discover/Home's "Upcoming Series" row. TMDB has no dedicated "upcoming TV" endpoint (only
   * movies get one) — this is {@code /discover/tv} filtered to a first-air-date in the future,
   * sorted soonest-first, which is the same query Overseerr/Jellyseerr use for the same row.
   *
   * <p>Unlike every other discover row, a large share of what this query surfaces — small/regional
   * productions ordered by "airs soonest" rather than any popularity signal — has no poster on TMDB
   * at all yet. Rather than showing a row half full of gray placeholder tiles, this pages through
   * results (capped at {@link #UPCOMING_TV_MAX_PAGES} TMDB calls) filtering to only posters-having
   * titles until it collects {@link #UPCOMING_TV_TARGET_COUNT} of them.
   */
  @CacheResult(cacheName = "tmdb-upcoming-tv")
  public List<MetadataSearchResult> fetchUpcomingTv(String excludeLanguages) {
    String extraParams =
        "sort_by=first_air_date.asc&first_air_date.gte=" + java.time.LocalDate.now();
    List<MetadataSearchResult> collected = new ArrayList<>();
    for (int page = 1;
        page <= UPCOMING_TV_MAX_PAGES && collected.size() < UPCOMING_TV_TARGET_COUNT;
        page++) {
      List<MetadataSearchResult> pageResults =
          fetchTvFiltered(
              DISCOVER_TV_URL,
              extraParams + "&page=" + page,
              excludeLanguages,
              "TMDB upcoming TV fetch failed");
      for (MetadataSearchResult r : pageResults) {
        if (r.posterPath() != null) {
          collected.add(r);
          if (collected.size() >= UPCOMING_TV_TARGET_COUNT) {
            break;
          }
        }
      }
    }
    return collected;
  }

  /**
   * Backs the "Upcoming Series" list page's infinite scroll beyond page 1 (which reuses {@link
   * #fetchUpcomingTv(String)}'s curated, posters-only page instead) — a single raw {@code
   * /discover/tv} page, same query, no posters-only filtering. Unlike the home row, a list page the
   * user is actively scrolling through can show the occasional posterless placeholder same as every
   * other paginated list here; TMDB genuinely doesn't have art for everything this query surfaces.
   */
  @CacheResult(cacheName = "tmdb-upcoming-tv-paged")
  public List<MetadataSearchResult> fetchUpcomingTv(int page, String excludeLanguages) {
    String extraParams =
        "sort_by=first_air_date.asc&first_air_date.gte="
            + java.time.LocalDate.now()
            + "&page="
            + page;
    return fetchTvFiltered(
        DISCOVER_TV_URL, extraParams, excludeLanguages, "TMDB upcoming TV fetch failed");
  }

  /** Backs Discover/Home's "Movie Genres" tile row — the fixed TMDB genre vocabulary for movies. */
  @CacheResult(cacheName = "tmdb-movie-genres")
  public List<TmdbGenre> fetchMovieGenres() {
    return fetchGenres(MOVIE_GENRES_URL, "TMDB movie genre list fetch failed");
  }

  /** Backs Discover/Home's "Series Genres" tile row — the fixed TMDB genre vocabulary for TV. */
  @CacheResult(cacheName = "tmdb-tv-genres")
  public List<TmdbGenre> fetchTvGenres() {
    return fetchGenres(TV_GENRES_URL, "TMDB TV genre list fetch failed");
  }

  private List<TmdbGenre> fetchGenres(String url, String failureMessage) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url + "?api_key=" + apiKey.orElseThrow()))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return objectMapper.readValue(response.body(), TmdbGenreListResponse.class).genres();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  /**
   * Backs a genre tile's click-through page and its infinite scroll and language filter — movies
   * tagged with the given TMDB genre id.
   */
  @CacheResult(cacheName = "tmdb-discover-movie-genre")
  public List<MetadataSearchResult> discoverMoviesByGenre(
      int genreId, int page, String excludeLanguages) {
    return fetchMoviesFiltered(
        DISCOVER_MOVIE_URL,
        "sort_by=popularity.desc&with_genres=" + genreId + "&page=" + page,
        excludeLanguages,
        "TMDB movie-by-genre discover failed");
  }

  /**
   * Backs a genre tile's click-through page and its infinite scroll and language filter — series
   * tagged with the given TMDB genre id.
   */
  @CacheResult(cacheName = "tmdb-discover-tv-genre")
  public List<MetadataSearchResult> discoverTvByGenre(
      int genreId, int page, String excludeLanguages) {
    return fetchTvFiltered(
        DISCOVER_TV_URL,
        "sort_by=popularity.desc&with_genres=" + genreId + "&page=" + page,
        excludeLanguages,
        "TMDB TV-by-genre discover failed");
  }

  /**
   * Backs a studio tile's click-through page and its infinite scroll and language filter — movies
   * produced by the given TMDB company id.
   */
  @CacheResult(cacheName = "tmdb-discover-movie-company")
  public List<MetadataSearchResult> discoverMoviesByCompany(
      int companyId, int page, String excludeLanguages) {
    return fetchMoviesFiltered(
        DISCOVER_MOVIE_URL,
        "sort_by=popularity.desc&with_companies=" + companyId + "&page=" + page,
        excludeLanguages,
        "TMDB movie-by-company discover failed");
  }

  /**
   * Backs a network tile's click-through page and its infinite scroll and language filter — series
   * airing on the given TMDB network id.
   */
  @CacheResult(cacheName = "tmdb-discover-tv-network")
  public List<MetadataSearchResult> discoverTvByNetwork(
      int networkId, int page, String excludeLanguages) {
    return fetchTvFiltered(
        DISCOVER_TV_URL,
        "sort_by=popularity.desc&with_networks=" + networkId + "&page=" + page,
        excludeLanguages,
        "TMDB TV-by-network discover failed");
  }

  /**
   * Fetch + typed-parse + language-filter + map for every movie list in this class — pulled out
   * once all of trending/popular/upcoming/genre/company movie lists needed the same "read {@code
   * original_language} before it's discarded on the way to {@link MetadataSearchResult}" shape.
   */
  private List<MetadataSearchResult> fetchMoviesFiltered(
      String url, String extraParams, String excludeLanguages, String failureMessage) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      String fullUrl =
          url
              + (url.contains("?") ? "&" : "?")
              + "api_key="
              + apiKey.orElseThrow()
              + (extraParams == null ? "" : "&" + extraParams);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      TmdbSearchResponse parsed = objectMapper.readValue(response.body(), TmdbSearchResponse.class);
      Set<String> excluded = parseLanguages(excludeLanguages);
      return parsed.results().stream()
          .filter(m -> !excluded.contains(m.originalLanguage()))
          .map(this::toSearchResult)
          .toList();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  /** TV counterpart of {@link #fetchMoviesFiltered}. */
  private List<MetadataSearchResult> fetchTvFiltered(
      String url, String extraParams, String excludeLanguages, String failureMessage) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      String fullUrl =
          url
              + (url.contains("?") ? "&" : "?")
              + "api_key="
              + apiKey.orElseThrow()
              + (extraParams == null ? "" : "&" + extraParams);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      TmdbTvSearchResponse parsed =
          objectMapper.readValue(response.body(), TmdbTvSearchResponse.class);
      Set<String> excluded = parseLanguages(excludeLanguages);
      return parsed.results().stream()
          .filter(s -> !excluded.contains(s.originalLanguage()))
          .map(this::toSearchResult)
          .toList();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  /**
   * TMDB's search endpoint doesn't return runtime — only {@code /movie/{id}} does — so this is a
   * second call, made once at movie-creation time to populate {@link
   * de.oppahansi.kosmos.media.Movie#runtimeMinutes} for the size-gate quality check ({@code
   * parsing.QualityDefinition}). Best-effort: any failure (network, missing/unconfigured key, a
   * movie TMDB genuinely has no runtime for) returns empty rather than failing the whole movie
   * creation over an enrichment call.
   *
   * <p>Not a repeated-call hot path today (called exactly once per movie, at creation) — cached
   * anyway, cheaply, so a double "Add" click or a retry after a transient failure doesn't re-hit
   * TMDB for data that's essentially permanent once a movie has released. 7 days, not 24h like
   * {@link #search}: this is far more static than search results.
   */
  @CacheResult(cacheName = "tmdb-movie-details")
  public Optional<Integer> fetchRuntimeMinutes(String tmdbId) {
    if (apiKey.isEmpty()) {
      return Optional.empty();
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(MOVIE_URL + tmdbId + "?api_key=" + apiKey.orElseThrow()))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Optional.empty();
      }
      TmdbMovieDetails details = objectMapper.readValue(response.body(), TmdbMovieDetails.class);
      return Optional.ofNullable(details.runtime()).filter(r -> r > 0);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Full details for one known TMDB movie id — backs {@code JellyfinSyncService}'s poster/backdrop
   * backfill: Jellyfin's own ProviderIds give a TMDB id but no artwork, so this is the one real
   * TMDB round trip that fills that gap in. Same response shape as {@link #search} bar the extra
   * fields {@link TmdbMovie} ignores, just unwrapped (a single object, not a {@code results} list).
   */
  @CacheResult(cacheName = "tmdb-movie-by-id")
  public Optional<MetadataSearchResult> fetchMovieById(String tmdbId) {
    if (apiKey.isEmpty()) {
      return Optional.empty();
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(MOVIE_URL + tmdbId + "?api_key=" + apiKey.orElseThrow()))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Optional.empty();
      }
      return Optional.of(toSearchResult(objectMapper.readValue(response.body(), TmdbMovie.class)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Show-level counterpart to {@link #fetchMovieById} — backs {@code JellyfinSyncService}'s show
   * sync, which (like movies) only has a bare TMDB id from Jellyfin's ProviderIds and needs the
   * poster/backdrop/overview {@link #fetchShowStructure} doesn't carry.
   */
  @CacheResult(cacheName = "tmdb-show-by-id")
  public Optional<MetadataSearchResult> fetchShowById(String tmdbId) {
    if (apiKey.isEmpty()) {
      return Optional.empty();
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(TV_URL + tmdbId + "?api_key=" + apiKey.orElseThrow()))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Optional.empty();
      }
      return Optional.of(toSearchResult(objectMapper.readValue(response.body(), TmdbTvShow.class)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Backs Discover/Home's "Because You Added" row — TMDB's own {@code /movie/{id}/recommendations},
   * same response shape as {@link #fetchTrendingMovies}/{@link #fetchPopularMovies}. Cached per
   * seed movie id rather than globally, since unlike trending/popular this genuinely varies by
   * which movie in the library it's computed from.
   */
  @CacheResult(cacheName = "tmdb-movie-recommendations")
  public List<MetadataSearchResult> fetchMovieRecommendations(String tmdbId) {
    return fetchList(MOVIE_URL + tmdbId + "/recommendations", "TMDB recommendations fetch failed");
  }

  private List<MetadataSearchResult> fetchList(String url, String failureMessage) {
    return fetch(url, null, failureMessage, this::parse);
  }

  private List<MetadataSearchResult> fetch(
      String url,
      String extraParams,
      String failureMessage,
      java.util.function.Function<String, List<MetadataSearchResult>> parser) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      String separator = url.contains("?") ? "&" : "?";
      String fullUrl =
          url
              + separator
              + "api_key="
              + apiKey.orElseThrow()
              + (extraParams == null ? "" : "&" + extraParams);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return parser.apply(response.body());
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  /**
   * TV-show search — same idea as {@link #search}, TMDB's shape just differs (name, not title).
   * Separate cache name from {@link #search} even though both are keyed by the same query string,
   * so a movie-only cache hit can't accidentally short-circuit the TV results for the same query.
   */
  @CacheResult(cacheName = "tmdb-search-tv")
  public List<MetadataSearchResult> searchTv(String query) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(
                  URI.create(
                      "%s?api_key=%s&query=%s"
                          .formatted(TV_SEARCH_URL, apiKey.orElseThrow(), encodedQuery)))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      TmdbTvSearchResponse parsed =
          objectMapper.readValue(response.body(), TmdbTvSearchResponse.class);
      return parsed.results().stream().map(this::toSearchResult).toList();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("TMDB TV search failed", e);
    }
  }

  /**
   * Fetches a show's full season/episode tree at show-creation time: one {@code /tv/{id}} call for
   * the season list, then one {@code /tv/{id}/season/{n}} call per season for its episodes. Unlike
   * {@link #fetchRuntimeMinutes}, this isn't best-effort — a show with no structure isn't a useful
   * show to have added, so failures propagate rather than silently creating an empty shell.
   *
   * <p>The per-season/per-details work is delegated to {@link TmdbTvDetailClient}, a separate bean
   * specifically so each piece is independently cacheable — see that class's own doc comment for
   * why (the short version: {@code @CacheResult} can't intercept same-class private-method calls,
   * and per-season caching gives a real resume-after-partial-failure property this method's own
   * whole-tree cache below can't). Both layers are kept deliberately: this cache serves an
   * immediate duplicate call instantly without even touching the inner caches; the inner caches are
   * what make a *retry after failure* cheap.
   */
  @CacheResult(cacheName = "tmdb-show-structure")
  public TmdbShowStructure fetchShowStructure(String tmdbId) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    TmdbTvDetails details = tmdbTvDetailClient.fetchTvDetails(tmdbId);
    List<TmdbShowStructure.SeasonData> seasons = new ArrayList<>();
    for (TmdbTvDetails.Season season : details.seasons()) {
      List<TmdbShowStructure.EpisodeData> episodes =
          tmdbTvDetailClient.fetchSeasonEpisodes(tmdbId, season.seasonNumber());
      seasons.add(
          new TmdbShowStructure.SeasonData(
              season.seasonNumber(),
              season.name(),
              season.overview(),
              season.posterPath(),
              season.episodeCount(),
              episodes));
    }
    return new TmdbShowStructure(details.status(), seasons);
  }

  /**
   * Exposes {@link TmdbTvDetailClient#fetchSeasonEpisodes} beyond this package — {@code
   * media.AnimeService} uses it to enrich an anime's episode tree with TMDB's real per-episode data
   * once Fribb/anime-lists has resolved the anime to a TMDB TV id + season number. {@code
   * TmdbTvDetailClient} itself stays package-private (it's an internal caching split, not a public
   * API — see its own doc comment); this is a thin pass-through, not a duplicate of its
   * {@code @CacheResult}, which still applies since the call crosses a real bean boundary.
   */
  public List<TmdbShowStructure.EpisodeData> fetchSeasonEpisodes(String tmdbId, int seasonNumber) {
    return tmdbTvDetailClient.fetchSeasonEpisodes(tmdbId, seasonNumber);
  }

  String buildSearchUrl(String query) {
    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
    return "%s?api_key=%s&query=%s".formatted(SEARCH_URL, apiKey.orElseThrow(), encodedQuery);
  }

  List<MetadataSearchResult> parse(String json) {
    try {
      TmdbSearchResponse response = objectMapper.readValue(json, TmdbSearchResponse.class);
      return response.results().stream().map(this::toSearchResult).toList();
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid TMDB response", e);
    }
  }

  private MetadataSearchResult toSearchResult(TmdbMovie movie) {
    return new MetadataSearchResult(
        String.valueOf(movie.id()),
        movie.title(),
        extractYear(movie.releaseDate()),
        movie.overview(),
        movie.posterPath(),
        movie.backdropPath(),
        movie.voteAverage(),
        "movie");
  }

  private MetadataSearchResult toSearchResult(TmdbTvShow show) {
    return new MetadataSearchResult(
        String.valueOf(show.id()),
        show.name(),
        extractYear(show.firstAirDate()),
        show.overview(),
        show.posterPath(),
        show.backdropPath(),
        null,
        "tv");
  }

  private Integer extractYear(String releaseDate) {
    if (releaseDate == null || releaseDate.length() < 4) {
      return null;
    }
    return Integer.valueOf(releaseDate.substring(0, 4));
  }
}
