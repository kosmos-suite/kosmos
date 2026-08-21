package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Single-row table holding the runtime override for kosmos.library.root-path, if any. */
@Entity
@Table(name = "library_setting")
public class LibrarySetting extends KosmosEntity {

  @Column(name = "root_path", length = 1000)
  public String rootPath;
}
