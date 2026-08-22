package de.oppahansi.kosmos.health;

import de.oppahansi.kosmos.indexers.Indexer;
import jakarta.enterprise.context.ApplicationScoped;

/** Nothing can ever be found to grab without at least one enabled indexer. */
@ApplicationScoped
public class IndexerHealthCheck implements HealthCheck {

  @Override
  public HealthCheckResult check() {
    long enabled = Indexer.count("enabled", true);
    if (enabled == 0) {
      return HealthCheckResult.error(
          "Indexers",
          "No enabled indexer — automatic and interactive search have nothing to query.");
    }
    return HealthCheckResult.ok("Indexers");
  }
}
