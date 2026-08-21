package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A physical media file on disk and how it was identified against a {@link MediaItem}. */
@Entity
@Table(name = "library_file")
public class LibraryFile extends KosmosEntity {

  @ManyToOne
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(nullable = false, unique = true, length = 1000)
  public String path;

  @Column(name = "size_bytes", nullable = false)
  public long sizeBytes;

  @Column(name = "match_method", nullable = false, length = 20)
  public String matchMethod;

  @Column(name = "match_confidence", nullable = false)
  public float matchConfidence;

  @Column(name = "match_pinned", nullable = false)
  public boolean matchPinned;

  @Column(name = "matched_at")
  public Instant matchedAt;

  @Column(length = 20)
  public String container;

  @Column(name = "video_codec", length = 30)
  public String videoCodec;

  @Column(name = "resolution_width")
  public Integer resolutionWidth;

  @Column(name = "resolution_height")
  public Integer resolutionHeight;

  @Column(name = "duration_seconds")
  public Integer durationSeconds;

  @Column(name = "hdr_format", length = 30)
  public String hdrFormat;

  @Column(name = "bit_depth")
  public Integer bitDepth;

  @Column(name = "probed_at")
  public Instant probedAt;

  @Column(name = "embedded_title", length = 500)
  public String embeddedTitle;

  @Column(nullable = false)
  public boolean verified;

  @Column(name = "imported_at", nullable = false)
  public Instant importedAt;
}
