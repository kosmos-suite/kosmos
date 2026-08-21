package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A raw external id tag (e.g. IMDb, TMDB) read from a {@link LibraryFile}'s container. */
@Entity
@Table(name = "library_file_embedded_id")
public class LibraryFileEmbeddedId extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "library_file_id")
  public LibraryFile libraryFile;

  @Column(nullable = false, length = 50)
  public String label;

  @Column(name = "raw_value", nullable = false, length = 200)
  public String rawValue;
}
