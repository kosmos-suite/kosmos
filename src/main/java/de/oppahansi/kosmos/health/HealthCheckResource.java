package de.oppahansi.kosmos.health;

import de.oppahansi.kosmos.health.dto.HealthCheckResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Deliberately not {@code /health} — that path is Quarkus's own SmallRye MicroProfile Health
 * endpoint ({@code /q/health}), a liveness/readiness probe. This is a distinct, admin-facing
 * concept: "is this instance configured to do its job," which can be false while the process itself
 * is perfectly alive.
 */
@Path("/system-checks")
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckResource {

  @Inject HealthCheckService healthCheckService;

  @GET
  public List<HealthCheckResponse> check() {
    return healthCheckService.runAll().stream().map(HealthCheckResponse::from).toList();
  }
}
