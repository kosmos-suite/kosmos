package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A registered library root folder — grabbed releases land under whichever one a title is assigned
 * to, the same "Root Folders" concept Radarr/Sonarr use.
 */
@Entity
@Table(name = "library_root_folder")
public class LibraryRootFolder extends KosmosEntity {

  @Column(nullable = false, length = 1000, unique = true)
  public String path;

  // Comma-separated MediaItem.contentType values this folder accepts ("movie", "show", "anime");
  // null/blank means no restriction, so it's offered for anything.
  @Column(name = "content_types", length = 200)
  public String contentTypes;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
