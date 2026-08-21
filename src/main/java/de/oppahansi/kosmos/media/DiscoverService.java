package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.BecauseYouAddedResult;
import de.oppahansi.kosmos.media.dto.DiscoverItem;
import de.oppahansi.kosmos.media.dto.GenreTile;
import de.oppahansi.kosmos.media.dto.StudioTile;
import de.oppahansi.kosmos.metadata.MediaItemExternalId;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Backs Discover/Home's real rows — see {@link DiscoverResource}. */
@ApplicationScoped
public class DiscoverService {

  private static final int RECENT_LIMIT = 12;

  @Inject TmdbMetadataProvider tmdbMetadataProvider;

  /**
   * 100% Kosmos's own data — no TMDB call, no caching needed (a Postgres query for a homelab-sized
   * library is already fast, and unlike trending/popular this must reflect additions immediately).
   */
  public List<DiscoverItem> recentlyAdded() {
    List<Movie> movies =
        Movie.<Movie>find("order by mediaItem.addedAt desc").page(0, RECENT_LIMIT).list();
    List<Show> shows =
        Show.<Show>find("order by mediaItem.addedAt desc").page(0, RECENT_LIMIT).list();

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
                                true))),
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
                                true))))
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
          tmdbMetadataProvider.fetchMovieRecommendations(link.get().externalId);
      if (!recommendations.isEmpty()) {
        return Optional.of(
            new BecauseYouAddedResult(
                movie.mediaItem.title, withLibraryStatus(recommendations, "movie")));
      }
    }
    return Optional.empty();
  }

  public List<DiscoverItem> trending() {
    return withLibraryStatus(tmdbMetadataProvider.fetchTrendingMovies(), "movie");
  }

  public List<DiscoverItem> popular() {
    return withLibraryStatus(tmdbMetadataProvider.fetchPopularMovies(), "movie");
  }

  public List<DiscoverItem> upcomingMovies() {
    return withLibraryStatus(tmdbMetadataProvider.fetchUpcomingMovies(), "movie");
  }

  public List<DiscoverItem> popularTv() {
    return withLibraryStatus(tmdbMetadataProvider.fetchPopularTv(), "show");
  }

  public List<DiscoverItem> upcomingTv() {
    return withLibraryStatus(tmdbMetadataProvider.fetchUpcomingTv(), "show");
  }

  public List<GenreTile> movieGenres() {
    return tmdbMetadataProvider.fetchMovieGenres().stream()
        .map(g -> new GenreTile(g.id(), g.name()))
        .toList();
  }

  public List<GenreTile> tvGenres() {
    return tmdbMetadataProvider.fetchTvGenres().stream()
        .map(g -> new GenreTile(g.id(), g.name()))
        .toList();
  }

  public List<DiscoverItem> moviesByGenre(int genreId) {
    return withLibraryStatus(tmdbMetadataProvider.discoverMoviesByGenre(genreId), "movie");
  }

  public List<DiscoverItem> tvByGenre(int genreId) {
    return withLibraryStatus(tmdbMetadataProvider.discoverTvByGenre(genreId), "show");
  }

  public List<StudioTile> studios() {
    return STUDIOS;
  }

  public List<StudioTile> networks() {
    return NETWORKS;
  }

  public List<DiscoverItem> moviesByStudio(int companyId) {
    return withLibraryStatus(tmdbMetadataProvider.discoverMoviesByCompany(companyId), "movie");
  }

  public List<DiscoverItem> tvByNetwork(int networkId) {
    return withLibraryStatus(tmdbMetadataProvider.discoverTvByNetwork(networkId), "show");
  }

  /**
   * Cross-references TMDB results against the library in one batched query — not per-item, so a
   * 20-result row costs one extra query, not twenty. {@code contentType} scopes the match to
   * "movie" or "show" as appropriate: without it, a movie and a show that happen to share the same
   * TMDB numeric id — different, unrelated namespaces — could otherwise be confused for each other.
   */
  private List<DiscoverItem> withLibraryStatus(
      List<MetadataSearchResult> results, String contentType) {
    List<String> externalIds = results.stream().map(MetadataSearchResult::externalId).toList();
    Map<String, UUID> inLibrary =
        MediaItemExternalId.<MediaItemExternalId>find(
                "plugin.slug = ?1 and externalId in ?2 and mediaItem.contentType = ?3"
                    + " and supersededAt is null",
                "tmdb",
                externalIds,
                contentType)
            .<MediaItemExternalId>list()
            .stream()
            .collect(Collectors.toMap(l -> l.externalId, l -> l.mediaItem.id, (a, b) -> a));

    return results.stream()
        .map(
            r ->
                new DiscoverItem(
                    inLibrary.get(r.externalId()),
                    r.externalId(),
                    r.title(),
                    r.year(),
                    r.overview(),
                    r.posterPath(),
                    r.backdropPath(),
                    r.voteAverage(),
                    r.mediaType(),
                    inLibrary.containsKey(r.externalId())))
        .toList();
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
