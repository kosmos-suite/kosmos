package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An audio or subtitle track probed from a {@link LibraryFile}. */
@Entity
@Table(name = "library_file_track")
public class LibraryFileTrack extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "library_file_id")
  public LibraryFile libraryFile;

  @Column(name = "track_type", nullable = false, length = 20)
  public String trackType;

  @Column(length = 30)
  public String codec;

  @Column(length = 10)
  public String language;

  public Integer channels;
}
