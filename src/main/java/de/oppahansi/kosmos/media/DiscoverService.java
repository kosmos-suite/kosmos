package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.BecauseYouAddedResult;
import de.oppahansi.kosmos.media.dto.DiscoverItem;
import de.oppahansi.kosmos.media.dto.GenreTile;
import de.oppahansi.kosmos.media.dto.StudioTile;
import de.oppahansi.kosmos.metadata.MediaItemExternalId;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbDiscoverClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Backs Discover/Home's real rows — see {@link DiscoverResource}. */
@ApplicationScoped
public class DiscoverService {

  private static final int RECENT_LIMIT = 12;

  @Inject TmdbDiscoverClient tmdbDiscoverClient;
  @Inject MediaAvailabilityService mediaAvailabilityService;

  /**
   * 100% Kosmos's own data — no TMDB call, no caching needed (a Postgres query for a homelab-sized
   * library is already fast, and unlike trending/popular this must reflect additions immediately).
   */
  public List<DiscoverItem> recentlyAdded() {
    List<Movie> movies =
        Movie.<Movie>find("order by mediaItem.addedAt desc").page(0, RECENT_LIMIT).list();
    List<Show> shows =
        Show.<Show>find("order by mediaItem.addedAt desc").page(0, RECENT_LIMIT).list();
    Set<UUID> partialShowIds =
        mediaAvailabilityService.partiallyAvailableShows(
            shows.stream().map(s -> s.mediaItemId).toList());

    record Ranked(Instant addedAt, DiscoverItem item) {}

    return java.util.stream.Stream.concat(
            movies.stream()
                .map(
                    m ->
                        new Ranked(
                            m.mediaItem.addedAt,
                            new DiscoverItem(
                                m.mediaItemId,
                                null,
                                m.mediaItem.title,
                                m.mediaItem.year,
                                m.overview,
                                m.posterPath,
                                m.backdropPath,
                                null,
                                "movie",
                                true,
                                false))),
            shows.stream()
                .map(
                    s ->
                        new Ranked(
                            s.mediaItem.addedAt,
                            new DiscoverItem(
                                s.mediaItemId,
                                null,
                                s.mediaItem.title,
                                s.mediaItem.year,
                                s.overview,
                                s.posterPath,
                                s.backdropPath,
                                null,
                                "tv",
                                true,
                                partialShowIds.contains(s.mediaItemId)))))
        .sorted((a, b) -> b.addedAt().compareTo(a.addedAt()))
        .limit(RECENT_LIMIT)
        .map(Ranked::item)
        .toList();
  }

  private static final int BECAUSE_YOU_ADDED_SEED_CANDIDATES = 5;

  /**
   * Picks the most recently added library movie that has both a TMDB link and at least one real
   * recommendation, and returns TMDB's own recommendations for it — tries a few of the most recent
   * additions in order rather than just the single latest one, since a niche title can genuinely
   * have no recommendations TMDB is willing to surface. Returns empty (row hidden entirely) if none
   * of the recent candidates work out, e.g. an empty library or no TMDB key configured.
   */
  public Optional<BecauseYouAddedResult> becauseYouAdded() {
    List<Movie> recentMovies =
        Movie.<Movie>find("order by mediaItem.addedAt desc")
            .page(0, BECAUSE_YOU_ADDED_SEED_CANDIDATES)
            .list();

    for (Movie movie : recentMovies) {
      Optional<MediaItemExternalId> link =
          MediaItemExternalId.find(
                  "plugin.slug = ?1 and mediaItem = ?2 and supersededAt is null",
                  "tmdb",
                  movie.mediaItem)
              .firstResultOptional();
      if (link.isEmpty()) {
        continue;
      }
      List<MetadataSearchResult> recommendations =
          tmdbDiscoverClient.fetchMovieRecommendations(link.get().externalId);
      if (!recommendations.isEmpty()) {
        return Optional.of(
            new BecauseYouAddedResult(
                movie.mediaItem.title, withLibraryStatus(recommendations, "movie")));
      }
    }
    return Optional.empty();
  }

  /**
   * Backs Discover/Home's hero and "Trending" row (default: mixed, week, page 1, unfiltered) and
   * the "Trending" list page's window/type/language filters and infinite scroll. {@code mediaType}
   * is {@code "all"} (TMDB's own mixed trending, movies and series together — matches Overseerr/
   * Jellyseerr's own Trending row), {@code "movie"}, or {@code "tv"}. {@code excludeLanguages} is a
   * comma-separated list of ISO 639-1 codes to drop by original language, or blank for no filter.
   */
  public List<DiscoverItem> trending(
      String window, String mediaType, int page, String excludeLanguages) {
    return switch (mediaType) {
      case "movie" ->
          withLibraryStatus(
              tmdbDiscoverClient.fetchTrendingMovies(window, page, excludeLanguages), "movie");
      case "tv" ->
          withLibraryStatus(
              tmdbDiscoverClient.fetchTrendingTv(window, page, excludeLanguages), "show");
      default ->
          withLibraryStatusMixed(
              tmdbDiscoverClient.fetchTrendingAll(window, page, excludeLanguages));
    };
  }

  public List<DiscoverItem> popular(int page, String excludeLanguages) {
    return withLibraryStatus(
        tmdbDiscoverClient.fetchPopularMovies(page, excludeLanguages), "movie");
  }

  public List<DiscoverItem> upcomingMovies(int page, String excludeLanguages) {
    return withLibraryStatus(
        tmdbDiscoverClient.fetchUpcomingMovies(page, excludeLanguages), "movie");
  }

  public List<DiscoverItem> popularTv(int page, String excludeLanguages) {
    return withLibraryStatus(tmdbDiscoverClient.fetchPopularTv(page, excludeLanguages), "show");
  }

  /**
   * Page 1 reuses {@link TmdbDiscoverClient#fetchUpcomingTv(String)}'s posters-only curation; later
   * pages (infinite scroll) are raw — see that method's own doc comment for why.
   */
  public List<DiscoverItem> upcomingTv(int page, String excludeLanguages) {
    List<MetadataSearchResult> results =
        page <= 1
            ? tmdbDiscoverClient.fetchUpcomingTv(excludeLanguages)
            : tmdbDiscoverClient.fetchUpcomingTv(page, excludeLanguages);
    return withLibraryStatus(results, "show");
  }

  public List<GenreTile> movieGenres() {
    return tmdbDiscoverClient.fetchMovieGenres().stream()
        .map(g -> new GenreTile(g.id(), g.name()))
        .toList();
  }

  public List<GenreTile> tvGenres() {
    return tmdbDiscoverClient.fetchTvGenres().stream()
        .map(g -> new GenreTile(g.id(), g.name()))
        .toList();
  }

  public List<DiscoverItem> moviesByGenre(int genreId, int page, String excludeLanguages) {
    return withLibraryStatus(
        tmdbDiscoverClient.discoverMoviesByGenre(genreId, page, excludeLanguages), "movie");
  }

  public List<DiscoverItem> tvByGenre(int genreId, int page, String excludeLanguages) {
    return withLibraryStatus(
        tmdbDiscoverClient.discoverTvByGenre(genreId, page, excludeLanguages), "show");
  }

  public List<StudioTile> studios() {
    return STUDIOS;
  }

  public List<StudioTile> networks() {
    return NETWORKS;
  }

  public List<DiscoverItem> moviesByStudio(int companyId, int page, String excludeLanguages) {
    return withLibraryStatus(
        tmdbDiscoverClient.discoverMoviesByCompany(companyId, page, excludeLanguages), "movie");
  }

  public List<DiscoverItem> tvByNetwork(int networkId, int page, String excludeLanguages) {
    return withLibraryStatus(
        tmdbDiscoverClient.discoverTvByNetwork(networkId, page, excludeLanguages), "show");
  }

  /**
   * Cross-references TMDB results against the library in one batched query — not per-item, so a
   * 20-result row costs one extra query, not twenty. {@code contentType} scopes the match to
   * "movie" or "show" as appropriate: without it, a movie and a show that happen to share the same
   * TMDB numeric id — different, unrelated namespaces — could otherwise be confused for each other.
   */
  private List<DiscoverItem> withLibraryStatus(
      List<MetadataSearchResult> results, String contentType) {
    List<MetadataSearchResult> deduped = dedupe(results);
    Map<String, UUID> inLibrary =
        lookupLibrary(deduped.stream().map(MetadataSearchResult::externalId).toList(), contentType);
    Set<UUID> partialIds =
        "show".equals(contentType)
            ? mediaAvailabilityService.partiallyAvailableShows(inLibrary.values())
            : Set.of();
    return deduped.stream().map(r -> toDiscoverItem(r, inLibrary, partialIds)).toList();
  }

  /**
   * Same idea as {@link #withLibraryStatus}, but for a mixed movie+TV list (trending's "all"
   * filter) where a single {@code contentType} can't be assumed per item — looks each item up
   * against the library bucket matching its own {@code mediaType} instead.
   */
  private List<DiscoverItem> withLibraryStatusMixed(List<MetadataSearchResult> results) {
    List<MetadataSearchResult> deduped = dedupe(results);
    List<String> movieIds =
        deduped.stream()
            .filter(r -> "movie".equals(r.mediaType()))
            .map(MetadataSearchResult::externalId)
            .toList();
    List<String> tvIds =
        deduped.stream()
            .filter(r -> "tv".equals(r.mediaType()))
            .map(MetadataSearchResult::externalId)
            .toList();
    Map<String, UUID> movieLibrary = lookupLibrary(movieIds, "movie");
    Map<String, UUID> tvLibrary = lookupLibrary(tvIds, "show");
    Set<UUID> partialTvIds = mediaAvailabilityService.partiallyAvailableShows(tvLibrary.values());
    return deduped.stream()
        .map(
            r ->
                "tv".equals(r.mediaType())
                    ? toDiscoverItem(r, tvLibrary, partialTvIds)
                    : toDiscoverItem(r, movieLibrary, Set.of()))
        .toList();
  }

  /**
   * TMDB's popularity-sorted lists aren't perfectly stable between requests — an item can shift
   * rank and end up reported on two different pages, or occasionally twice in the same response
   * (seen with {@code with_genres} ties). Keyed by ({@code mediaType}, {@code externalId}), the
   * only stable identity a TMDB result has; first occurrence wins.
   */
  private List<MetadataSearchResult> dedupe(List<MetadataSearchResult> results) {
    Map<String, MetadataSearchResult> byKey = new LinkedHashMap<>();
    for (MetadataSearchResult r : results) {
      byKey.putIfAbsent(r.mediaType() + ":" + r.externalId(), r);
    }
    return List.copyOf(byKey.values());
  }

  private Map<String, UUID> lookupLibrary(List<String> externalIds, String contentType) {
    if (externalIds.isEmpty()) {
      return Map.of();
    }
    return MediaItemExternalId.<MediaItemExternalId>find(
            "plugin.slug = ?1 and externalId in ?2 and mediaItem.contentType = ?3"
                + " and supersededAt is null",
            "tmdb",
            externalIds,
            contentType)
        .<MediaItemExternalId>list()
        .stream()
        .collect(Collectors.toMap(l -> l.externalId, l -> l.mediaItem.id, (a, b) -> a));
  }

  private DiscoverItem toDiscoverItem(
      MetadataSearchResult r, Map<String, UUID> inLibrary, Set<UUID> partialIds) {
    UUID mediaItemId = inLibrary.get(r.externalId());
    return new DiscoverItem(
        mediaItemId,
        r.externalId(),
        r.title(),
        r.year(),
        r.overview(),
        r.posterPath(),
        r.backdropPath(),
        r.voteAverage(),
        r.mediaType(),
        mediaItemId != null,
        mediaItemId != null && partialIds.contains(mediaItemId));
  }

  /**
   * Curated, hardcoded — same approach Overseerr/Jellyseerr use: a fixed marquee of well-known
   * studios rather than trying to derive "notable" from TMDB data. IDs/logo paths verified directly
   * against the TMDB API.
   */
  private static final List<StudioTile> STUDIOS =
      List.of(
          new StudioTile(2, "Walt Disney Pictures", "/wdrCwmRnLFJhEoH8GSfymY85KHT.png"),
          new StudioTile(420, "Marvel Studios", "/hUzeosd33nzE5MCNsZxCGEKTXaQ.png"),
          new StudioTile(429, "DC", "/4Y00XuSMuP1gimd0jP6JT57QbCI.png"),
          new StudioTile(3, "Pixar", "/1TjvGVDMYsj6JBxOAkUHpPEwLf7.png"),
          new StudioTile(174, "Warner Bros. Pictures", "/zhD3hhtKB5qyv7ZeL4uLpNxgMVU.png"),
          new StudioTile(33, "Universal Pictures", "/8lvHyhjr8oUKOOy2dKXoALWKdp0.png"),
          new StudioTile(4, "Paramount Pictures", "/jay6WcMgagAklUt7i9Euwj1pzTF.png"),
          new StudioTile(5, "Columbia Pictures", "/71BqEFAF4V3qjjMPCpLuyJFB9A.png"),
          new StudioTile(1632, "Lionsgate", "/cisLn1YAUuptXVBa0xjq7ST9cH0.png"),
          new StudioTile(41077, "A24", "/1ZXsGaFPgrgS6ZZGS37AqD5uU12.png"),
          new StudioTile(923, "Legendary Pictures", "/5UQsZrfbfG2dYJbx8DxfoTr2Bvu.png"),
          new StudioTile(25, "20th Century Fox", "/nM2MfoMqzJQRiSynsDabOtFKetD.png"));

  private static final List<StudioTile> NETWORKS =
      List.of(
          new StudioTile(213, "Netflix", "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png"),
          new StudioTile(2739, "Disney+", "/1edZOYAfoyZyZ3rklNSiUpXX30Q.png"),
          new StudioTile(1024, "Prime Video", "/w7HfLNm9CWwRmAMU58udl2L7We7.png"),
          new StudioTile(3186, "HBO Max", "/nmU0UMDJB3dRRQSTUqawzF2Od1a.png"),
          new StudioTile(2552, "Apple TV+", "/bngHRFi794mnMq34gfVcm9nDxN1.png"),
          new StudioTile(453, "Hulu", "/pqUTCleNUiTLAVlelGxUgWn1ELh.png"),
          new StudioTile(88, "FX", "/aexGjtcs42DgRtZh7zOxayiry4J.png"),
          new StudioTile(4, "BBC One", "/uJjcCg3O4DMEjM0xtno9OWFciRP.png"));
}
