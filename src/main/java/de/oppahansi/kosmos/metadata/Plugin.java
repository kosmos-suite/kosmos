package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A registered metadata or identity source, built-in or community-supplied. */
@Entity
@Table(name = "plugin")
public class Plugin extends KosmosEntity {

  @Column(nullable = false, unique = true, length = 100)
  public String slug;

  @Column(nullable = false, length = 200)
  public String name;

  @Column(nullable = false, length = 20)
  public String kind;

  @Column(name = "built_in", nullable = false)
  public boolean builtIn;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "installed_at", nullable = false)
  public Instant installedAt;
}
