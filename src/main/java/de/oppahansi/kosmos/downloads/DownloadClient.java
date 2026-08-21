package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A configured download client. */
@Entity
@Table(name = "download_client")
public class DownloadClient extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  /** Only {@code "qbittorrent"} is implemented; the column is free-form for future clients. */
  @Column(nullable = false, length = 30)
  public String type;

  @Column(name = "base_url", nullable = false, length = 1000)
  public String baseUrl;

  @Column(length = 200)
  public String username;

  @Column(length = 200)
  public String password;

  @Column(length = 100)
  public String category;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
