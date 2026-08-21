package de.oppahansi.kosmos.media;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Whether a show/anime that's in the library has every episode's file, or only some — the
 * Jellyseerr/Seerr "Partially Available" distinction, which Kosmos didn't track at all before (a
 * show/anime showed the same in-library check regardless of how much of it was actually
 * downloaded). Movies have no equivalent: they're a single file, so "in library" already means
 * "complete" for them.
 */
@ApplicationScoped
public class MediaAvailabilityService {

  @PersistenceContext EntityManager em;

  /** Show media item ids, from {@code mediaItemIds}, that have some but not all episode files. */
  public Set<UUID> partiallyAvailableShows(Collection<UUID> mediaItemIds) {
    if (mediaItemIds.isEmpty()) {
      return Set.of();
    }
    List<Object[]> rows =
        em.createNativeQuery(
                "select se.show_id, count(e.media_item_id), count(lf.media_item_id) "
                    + "from season se "
                    + "join episode e on e.season_id = se.id "
                    + "left join library_file lf on lf.media_item_id = e.media_item_id "
                    + "where se.show_id in (:ids) "
                    + "group by se.show_id")
            .setParameter("ids", toStrings(mediaItemIds))
            .getResultList();
    return partialIdsFrom(rows);
  }

  /** Anime media item ids, from {@code mediaItemIds}, that have some but not all episode files. */
  public Set<UUID> partiallyAvailableAnime(Collection<UUID> mediaItemIds) {
    if (mediaItemIds.isEmpty()) {
      return Set.of();
    }
    List<Object[]> rows =
        em.createNativeQuery(
                "select ae.anime_id, count(ae.media_item_id), count(lf.media_item_id) "
                    + "from anime_episode ae "
                    + "left join library_file lf on lf.media_item_id = ae.media_item_id "
                    + "where ae.anime_id in (:ids) "
                    + "group by ae.anime_id")
            .setParameter("ids", toStrings(mediaItemIds))
            .getResultList();
    return partialIdsFrom(rows);
  }

  /** {@code media_item_id} columns are stored as {@code varchar}, not native {@code uuid}. */
  private List<String> toStrings(Collection<UUID> ids) {
    return ids.stream().map(UUID::toString).toList();
  }

  private Set<UUID> partialIdsFrom(List<Object[]> rows) {
    Set<UUID> partial = new HashSet<>();
    for (Object[] row : rows) {
      long total = ((Number) row[1]).longValue();
      long owned = ((Number) row[2]).longValue();
      if (owned > 0 && owned < total) {
        partial.add(UUID.fromString(row[0].toString()));
      }
    }
    return partial;
  }
}
