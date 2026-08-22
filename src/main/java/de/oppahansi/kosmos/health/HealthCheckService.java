package de.oppahansi.kosmos.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;

/** Runs every registered {@link HealthCheck} — see {@link HealthCheckResource}. */
@ApplicationScoped
public class HealthCheckService {

  @Inject Instance<HealthCheck> checks;

  public List<HealthCheckResult> runAll() {
    return checks.stream().map(HealthCheck::check).toList();
  }
}
