package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Tracks the current best-scoring release {@code AutomaticSearchJob} has seen for one media item,
 * so a {@link de.oppahansi.kosmos.parsing.QualityProfile#grabDelayMinutes} delay can be honored
 * across multiple job runs — the job itself has no other memory of "have I seen this before"
 * between ticks. One row per media item: a differently-URLed candidate replacing the tracked one
 * resets the clock (a genuinely different release appeared, not the same one seen again), removed
 * once its media item is actually grabbed.
 */
@Entity
@Table(
    name = "pending_candidate",
    uniqueConstraints = @UniqueConstraint(columnNames = "media_item_id"))
public class PendingCandidate extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(name = "download_url", nullable = false, length = 2000)
  public String downloadUrl;

  @Column(name = "first_seen_at", nullable = false)
  public Instant firstSeenAt;
}
