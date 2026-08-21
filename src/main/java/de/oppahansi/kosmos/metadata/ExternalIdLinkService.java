package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.media.MediaItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Links a {@link MediaItem} to the external id it was matched from — shared by Movie and Show
 * creation.
 */
@ApplicationScoped
public class ExternalIdLinkService {

  /**
   * Whether {@code externalId} is already linked to a {@link MediaItem} — used to avoid creating a
   * duplicate.
   */
  public Optional<MediaItem> findLinkedMediaItem(String pluginSlug, String externalId) {
    return MediaItemExternalId.<MediaItemExternalId>find(
            "plugin.slug = ?1 and externalId = ?2 and supersededAt is null", pluginSlug, externalId)
        .firstResultOptional()
        .map(link -> link.mediaItem);
  }

  /**
   * The inverse of {@link #findLinkedMediaItem} — a {@link MediaItem}'s current external id on a
   * given plugin, e.g. the TMDB id a {@code Movie}/{@code Show} row was matched from. Shared by
   * {@code MovieService}/{@code ShowService}/{@code AnimeService}'s {@code detailExtras(UUID)},
   * which all previously hand-rolled the same JPQL shape with only the plugin slug literal
   * differing.
   */
  public Optional<String> findActiveExternalId(UUID mediaItemId, String pluginSlug) {
    return MediaItemExternalId.<MediaItemExternalId>find(
            "mediaItem.id = ?1 and plugin.slug = ?2 and supersededAt is null",
            mediaItemId,
            pluginSlug)
        .firstResultOptional()
        .map(link -> link.externalId);
  }

  @Transactional
  public void link(MediaItem mediaItem, String pluginSlug, String externalId) {
    Plugin plugin = findOrCreatePlugin(pluginSlug);

    MediaItemExternalId link = new MediaItemExternalId();
    link.mediaItem = mediaItem;
    link.plugin = plugin;
    link.externalId = externalId;
    link.matchedAt = Instant.now();
    link.persist();
  }

  private Plugin findOrCreatePlugin(String slug) {
    return Plugin.<Plugin>find("slug", slug)
        .firstResultOptional()
        .orElseGet(
            () -> {
              Plugin plugin = new Plugin();
              plugin.slug = slug;
              plugin.name = slug.toUpperCase();
              plugin.kind = "metadata";
              plugin.builtIn = true;
              plugin.enabled = true;
              plugin.installedAt = Instant.now();
              plugin.persist();
              return plugin;
            });
  }
}
