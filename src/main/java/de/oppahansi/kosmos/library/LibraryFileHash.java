package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A checksum computed for a {@link LibraryFile} (e.g. ed2k, CRC32, SHA-256). */
@Entity
@Table(name = "library_file_hash")
public class LibraryFileHash extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "library_file_id")
  public LibraryFile libraryFile;

  @Column(nullable = false, length = 20)
  public String algorithm;

  @Column(nullable = false, length = 200)
  public String value;
}
