package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.media.MediaAvailabilityService;
import de.oppahansi.kosmos.metadata.anilist.AniListMetadataProvider;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchItem;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs Search — movie, TV, and anime results merged, each cross-referenced against the library so
 * the UI can show "Already in your library" / a real link instead of unconditionally offering "Add"
 * on something already there.
 */
@ApplicationScoped
public class MetadataSearchService {

  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject AniListMetadataProvider aniListMetadataProvider;
  @Inject MediaAvailabilityService mediaAvailabilityService;

  public List<MetadataSearchItem> search(String query) {
    List<MetadataSearchResult> movies = tmdbMetadataProvider.search(query);
    List<MetadataSearchResult> shows = tmdbMetadataProvider.searchTv(query);
    List<MetadataSearchResult> anime = aniListMetadataProvider.search(query);

    Map<String, UUID> movieLibrary = lookupLibrary("tmdb", movies, "movie");
    Map<String, UUID> showLibrary = lookupLibrary("tmdb", shows, "show");
    Map<String, UUID> animeLibrary = lookupLibrary("anilist", anime, "anime");
    Set<UUID> partialShowIds =
        mediaAvailabilityService.partiallyAvailableShows(showLibrary.values());
    Set<UUID> partialAnimeIds =
        mediaAvailabilityService.partiallyAvailableAnime(animeLibrary.values());

    List<MetadataSearchItem> results = new ArrayList<>();
    movies.forEach(r -> results.add(toItem(r, movieLibrary, Set.of())));
    shows.forEach(r -> results.add(toItem(r, showLibrary, partialShowIds)));
    anime.forEach(r -> results.add(toItem(r, animeLibrary, partialAnimeIds)));
    return results;
  }

  private Map<String, UUID> lookupLibrary(
      String pluginSlug, List<MetadataSearchResult> results, String contentType) {
    List<String> externalIds = results.stream().map(MetadataSearchResult::externalId).toList();
    if (externalIds.isEmpty()) {
      return Map.of();
    }
    return MediaItemExternalId.<MediaItemExternalId>find(
            "plugin.slug = ?1 and externalId in ?2 and mediaItem.contentType = ?3"
                + " and supersededAt is null",
            pluginSlug,
            externalIds,
            contentType)
        .<MediaItemExternalId>list()
        .stream()
        .collect(Collectors.toMap(l -> l.externalId, l -> l.mediaItem.id, (a, b) -> a));
  }

  private MetadataSearchItem toItem(
      MetadataSearchResult r, Map<String, UUID> inLibrary, Set<UUID> partialIds) {
    UUID mediaItemId = inLibrary.get(r.externalId());
    return new MetadataSearchItem(
        mediaItemId,
        r.externalId(),
        r.title(),
        r.year(),
        r.overview(),
        r.posterPath(),
        r.backdropPath(),
        r.voteAverage(),
        r.mediaType(),
        r.episodeCount(),
        mediaItemId != null,
        mediaItemId != null && partialIds.contains(mediaItemId));
  }
}
