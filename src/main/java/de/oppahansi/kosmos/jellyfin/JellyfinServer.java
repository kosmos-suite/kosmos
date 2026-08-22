package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A configured Jellyfin server to sync an already-scanned library from. */
@Entity
@Table(name = "jellyfin_server")
public class JellyfinServer extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  @Column(name = "base_url", nullable = false, length = 1000)
  public String baseUrl;

  @Column(name = "api_key", nullable = false, length = 200)
  public String apiKey;

  // Comma-separated Jellyfin library (ItemId) ids; null/blank means "sync every library".
  @Column(name = "selected_library_ids", length = 2000)
  public String selectedLibraryIds;

  // Comma-separated Jellyfin user ids; null/blank means "import every account".
  @Column(name = "selected_user_ids", length = 4000)
  public String selectedUserIds;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
