package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.downloads.Grab;
import de.oppahansi.kosmos.library.LibraryFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes a MISSING/GRABBED/IMPORTED/AVAILABLE status per {@code MediaItem} id — shared by {@link
 * ShowResource} and {@link AnimeResource}, both of which need it for a whole tree of episodes at
 * once rather than one at a time.
 */
final class MediaItemStatus {

  private MediaItemStatus() {}

  /**
   * AVAILABLE (a {@link LibraryFile} exists) always wins over a {@link Grab}'s own status — same
   * "trust the file, not the record" precedence {@code MovieDetailPage} uses. One batched query per
   * table rather than one per episode, since a season (or an anime's whole episode count) can
   * easily be a few dozen of these.
   */
  static Map<UUID, String> forMediaItems(List<UUID> mediaItemIds) {
    if (mediaItemIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> status = new HashMap<>();
    Grab.<Grab>find("release.mediaItem.id in ?1", mediaItemIds)
        .list()
        .forEach(grab -> status.put(grab.release.mediaItem.id, grab.status));
    LibraryFile.<LibraryFile>find("mediaItem.id in ?1", mediaItemIds)
        .list()
        .forEach(file -> status.put(file.mediaItem.id, "AVAILABLE"));
    return status;
  }
}
