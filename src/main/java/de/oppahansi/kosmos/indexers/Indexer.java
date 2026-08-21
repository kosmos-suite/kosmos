package de.oppahansi.kosmos.indexers;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A configured Torznab-compatible indexer. */
@Entity
@Table(name = "indexer")
public class Indexer extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  @Column(name = "base_url", nullable = false, length = 1000)
  public String baseUrl;

  @Column(name = "api_key", nullable = false, length = 200)
  public String apiKey;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
