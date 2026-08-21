package de.oppahansi.kosmos.indexers;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A candidate release found for a {@link MediaItem}, before or after grabbing. */
@Entity
@Table(name = "release")
public class Release extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(name = "title_raw", nullable = false, length = 500)
  public String titleRaw;

  @Column(name = "download_url", nullable = false, length = 2000)
  public String downloadUrl;

  @Column(name = "parsed_resolution", length = 20)
  public String parsedResolution;

  @Column(name = "parsed_codec", length = 30)
  public String parsedCodec;

  @Column(name = "parsed_source", length = 30)
  public String parsedSource;

  public Integer score;

  @Column(name = "found_at", nullable = false)
  public Instant foundAt;
}
