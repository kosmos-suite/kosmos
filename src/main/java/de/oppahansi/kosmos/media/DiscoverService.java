package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.DiscoverItem;
import de.oppahansi.kosmos.metadata.MediaItemExternalId;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

  public List<DiscoverItem> trending() {
    return withLibraryStatus(tmdbMetadataProvider.fetchTrendingMovies());
  }

  public List<DiscoverItem> popular() {
    return withLibraryStatus(tmdbMetadataProvider.fetchPopularMovies());
  }

  /**
   * Cross-references TMDB results against the library in one batched query — not per-item, so a
   * 20-result trending/popular list costs one extra query, not twenty. Scoped to {@code contentType
   * = 'movie'}: trending/popular are movie-only for now (TV trending is a later, separate pass),
   * and without this a movie and a show that happen to share the same TMDB numeric id — different,
   * unrelated namespaces — could otherwise be confused for each other.
   */
  private List<DiscoverItem> withLibraryStatus(List<MetadataSearchResult> results) {
    List<String> externalIds = results.stream().map(MetadataSearchResult::externalId).toList();
    Map<String, UUID> inLibrary =
        MediaItemExternalId.<MediaItemExternalId>find(
                "plugin.slug = ?1 and externalId in ?2 and mediaItem.contentType = 'movie'"
                    + " and supersededAt is null",
                "tmdb",
                externalIds)
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
}
