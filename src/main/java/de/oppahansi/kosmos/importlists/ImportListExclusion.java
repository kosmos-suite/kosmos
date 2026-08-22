package de.oppahansi.kosmos.importlists;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * A permanent "never suggest this title again" — checked by {@link ImportListService#sync} before
 * filing a request for any candidate, across every list. Ports directly from Radarr/Sonarr's own
 * List Exclusions with no reframing needed, unlike the rest of this package.
 */
@Entity
@Table(
    name = "import_list_exclusion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"plugin_slug", "external_id"}))
public class ImportListExclusion extends KosmosEntity {

  @Column(name = "plugin_slug", nullable = false, length = 100)
  public String pluginSlug;

  @Column(name = "external_id", nullable = false, length = 200)
  public String externalId;

  @Column(nullable = false, length = 500)
  public String title;

  @Column(name = "excluded_at", nullable = false)
  public Instant excludedAt;
}
