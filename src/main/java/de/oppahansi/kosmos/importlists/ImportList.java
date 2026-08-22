package de.oppahansi.kosmos.importlists;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.parsing.QualityProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A configured list feed. Syncing it (see {@link ImportListSyncJob}) files a {@link
 * de.oppahansi.kosmos.requests.Request} for each new candidate — the existing approval queue, not a
 * silent add — unless {@link #trusted} is set, which auto-approves immediately. This is the
 * deliberate "Kosmos way" divergence from Radarr/Sonarr's own import lists, which always auto-add.
 */
@Entity
@Table(name = "import_list")
public class ImportList extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  @Column(name = "source_type", nullable = false, length = 50)
  public String sourceType;

  @Column(nullable = false)
  public boolean enabled;

  /** Files requests already-approved rather than pending review. Defaults off. */
  @Column(nullable = false)
  public boolean trusted;

  @ManyToOne
  @JoinColumn(name = "quality_profile_id")
  public QualityProfile qualityProfile;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "last_synced_at")
  public Instant lastSyncedAt;

  public ImportListSourceType parsedSourceType() {
    return ImportListSourceType.valueOf(sourceType);
  }
}
