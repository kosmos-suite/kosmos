package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Base identity shared by all content types (movie, show, episode, anime). */
@Entity
@Table(name = "media_item")
public class MediaItem extends KosmosEntity {

  @Column(name = "content_type", nullable = false, length = 20)
  public String contentType;

  @Column(nullable = false, length = 500)
  public String title;

  public Integer year;

  @Column(name = "added_at", nullable = false)
  public Instant addedAt;
}
