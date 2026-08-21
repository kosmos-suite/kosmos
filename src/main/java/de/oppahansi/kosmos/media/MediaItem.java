package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.library.LibraryRootFolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  /**
   * Where a grab for this title gets organized to. Episode/anime_episode rows carry a copy of their
   * parent show/anime's value, set once at creation, rather than looking it up via a relation
   * ImportService would otherwise need to join through.
   */
  @ManyToOne
  @JoinColumn(name = "root_folder_id")
  public LibraryRootFolder rootFolder;
}
