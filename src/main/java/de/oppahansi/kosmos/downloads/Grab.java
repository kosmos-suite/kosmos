package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.indexers.Release;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Records that a {@link Release} was sent to a {@link DownloadClient}. {@code status} starts as
 * {@code GRABBED} and moves to {@code IMPORTED} once {@code DownloadStatusPollJob} sees the
 * download complete and the file lands in the library.
 */
@Entity
@Table(name = "grab")
public class Grab extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "release_id")
  public Release release;

  @ManyToOne(optional = false)
  @JoinColumn(name = "download_client_id")
  public DownloadClient downloadClient;

  /** Info-hash for a torrent grab, job/queue id for a Usenet one — see {@link GrabService}. */
  @Column(name = "job_id", length = 100)
  public String jobId;

  @Column(nullable = false, length = 30)
  public String status;

  @Column(name = "grabbed_at", nullable = false)
  public Instant grabbedAt;
}
