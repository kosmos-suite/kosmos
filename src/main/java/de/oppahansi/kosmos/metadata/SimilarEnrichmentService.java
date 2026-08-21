package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.media.MediaAvailabilityService;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cross-references a detail page's "More Like This" row against the library — the same batched
 * lookup {@code media.DiscoverService}/{@code metadata.MetadataSearchService} do for their own
 * rows, factored out here since it's now needed a third time (movie/show/anime detail extras), each
 * against a single content type ({@code similar} is always homogeneous — a movie's recommendations
 * are all movies, an anime's are all anime).
 */
@ApplicationScoped
public class SimilarEnrichmentService {

  @Inject MediaAvailabilityService mediaAvailabilityService;

  public List<MetadataSearchItem> enrich(
      List<MetadataSearchItem> similar, String pluginSlug, String contentType) {
    if (similar.isEmpty()) {
      return similar;
    }
    List<String> externalIds = similar.stream().map(MetadataSearchItem::externalId).toList();
    Map<String, UUID> library =
        MediaItemExternalId.<MediaItemExternalId>find(
                "plugin.slug = ?1 and externalId in ?2 and mediaItem.contentType = ?3"
                    + " and supersededAt is null",
                pluginSlug,
                externalIds,
                contentType)
            .<MediaItemExternalId>list()
            .stream()
            .collect(Collectors.toMap(l -> l.externalId, l -> l.mediaItem.id, (a, b) -> a));

    Set<UUID> partialIds =
        switch (contentType) {
          case "show" -> mediaAvailabilityService.partiallyAvailableShows(library.values());
          case "anime" -> mediaAvailabilityService.partiallyAvailableAnime(library.values());
          default -> Set.of();
        };

    return similar.stream()
        .map(
            item -> {
              UUID mediaItemId = library.get(item.externalId());
              if (mediaItemId == null) {
                return item;
              }
              return new MetadataSearchItem(
                  mediaItemId,
                  item.externalId(),
                  item.title(),
                  item.year(),
                  item.overview(),
                  item.posterPath(),
                  item.backdropPath(),
                  item.voteAverage(),
                  item.mediaType(),
                  true,
                  partialIds.contains(mediaItemId));
            })
        .toList();
  }
}
