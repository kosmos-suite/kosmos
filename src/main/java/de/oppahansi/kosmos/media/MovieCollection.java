package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.parsing.QualityProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A TMDB movie collection Kosmos knows about — created the moment a user monitors one, since
 * there's otherwise no reason to persist a row for a collection nobody cares about (unlike a movie,
 * where the row itself is the point). {@link #monitored} gates {@code MovieCollectionSyncJob}
 * filing a Request per missing member, the same "Requests, not silent auto-add" shape Phase 07's
 * import lists use.
 */
@Entity
@Table(name = "movie_collection")
public class MovieCollection extends KosmosEntity {

  @Column(name = "tmdb_collection_id", nullable = false, unique = true, length = 50)
  public String tmdbCollectionId;

  @Column(nullable = false, length = 500)
  public String name;

  @Column(name = "poster_path", length = 500)
  public String posterPath;

  @Column(name = "backdrop_path", length = 500)
  public String backdropPath;

  @Column(nullable = false)
  public boolean monitored;

  @ManyToOne
  @JoinColumn(name = "quality_profile_id")
  public QualityProfile qualityProfile;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "last_synced_at")
  public Instant lastSyncedAt;
}
