package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Cross-reference between a {@link MediaItem} and its id in a {@link Plugin}'s catalog. */
@Entity
@Table(name = "media_item_external_id")
public class MediaItemExternalId extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @ManyToOne(optional = false)
  @JoinColumn(name = "plugin_id")
  public Plugin plugin;

  @Column(name = "external_id", nullable = false, length = 200)
  public String externalId;

  @Column(name = "matched_at", nullable = false)
  public Instant matchedAt;

  @Column(name = "superseded_at")
  public Instant supersededAt;
}
