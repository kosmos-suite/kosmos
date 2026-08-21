package de.oppahansi.kosmos.metadata.tmdb;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Searches movies and TV shows via the TMDB v3 API. */
@ApplicationScoped
public class TmdbMetadataProvider implements MetadataProvider {

  private static final String SEARCH_URL = "https://api.themoviedb.org/3/search/movie";
  private static final String TV_SEARCH_URL = "https://api.themoviedb.org/3/search/tv";
  private static final String MOVIE_URL = "https://api.themoviedb.org/3/movie/";
  private static final String TRENDING_URL = "https://api.themoviedb.org/3/trending/movie/week";
  private static final String POPULAR_URL = "https://api.themoviedb.org/3/movie/popular";

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
   * Backs Discover/Home's "Trending This Week" row ({@code media.DiscoverResource}). Same response
   * shape as {@link #search} (reuses {@link #parse}), just a different, unauthenticated-by-query
   * endpoint. Cached more aggressively than search: every single Home page load hits this exact
   * same query, with nothing user-specific about it, unlike free-text search where the cache key
   * varies per query string. 12h — trending lists are realistically that stable day to day.
   */
  @CacheResult(cacheName = "tmdb-trending")
  public List<MetadataSearchResult> fetchTrendingMovies() {
    return fetchList(TRENDING_URL, "TMDB trending fetch failed");
  }

  /**
   * Backs Discover/Home's "Popular Movies" row — same reasoning as {@link #fetchTrendingMovies}.
   */
  @CacheResult(cacheName = "tmdb-popular")
  public List<MetadataSearchResult> fetchPopularMovies() {
    return fetchList(POPULAR_URL, "TMDB popular fetch failed");
  }

  private List<MetadataSearchResult> fetchList(String url, String failureMessage) {
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
      return parse(response.body());
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
