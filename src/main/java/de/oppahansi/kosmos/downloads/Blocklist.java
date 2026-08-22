package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A release confirmed dead for one specific {@link MediaItem} — either {@link
 * DownloadStatusPollJob} detecting a download-client failure, or a user's manual "mark as failed."
 * Scoped per item rather than global: the same release URL being wrong for one title says nothing
 * about any other, so there's no reason to shut it out everywhere.
 */
@Entity
@Table(name = "blocklist")
public class Blocklist extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(name = "download_url", nullable = false, length = 2000)
  public String downloadUrl;

  @Column(name = "title_raw", nullable = false, length = 500)
  public String titleRaw;

  @Column(nullable = false, length = 500)
  public String reason;

  @Column(name = "blocked_at", nullable = false)
  public Instant blockedAt;
}
