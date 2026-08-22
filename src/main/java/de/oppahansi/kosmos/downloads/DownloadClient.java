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

  @Column(name = "remote_path", length = 1000)
  public String remotePath;

  @Column(name = "local_path", length = 1000)
  public String localPath;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  /**
   * Rewrites a content path this client reported into the path Kosmos's own filesystem sees, when
   * {@link #remotePath}/{@link #localPath} are configured and the path actually starts under {@link
   * #remotePath}. Returns the path unchanged otherwise — the common case, and always the case
   * before either field was ever set.
   */
  public String remapPath(String contentPath) {
    if (remotePath == null || remotePath.isBlank() || localPath == null || localPath.isBlank()) {
      return contentPath;
    }
    if (contentPath == null || !contentPath.startsWith(remotePath)) {
      return contentPath;
    }
    return localPath + contentPath.substring(remotePath.length());
  }
}
