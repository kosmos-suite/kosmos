package de.oppahansi.kosmos.health;

/**
 * A single operational/configuration advisory — deliberately separate from Quarkus's own
 * SmallRye/MicroProfile {@code /q/health} (process liveness/readiness for an orchestrator). These
 * checks answer a different question: "is this instance set up to actually do its job," which can
 * be true (WARNING/ERROR) while the process itself is perfectly up. Implementations are CDI beans,
 * discovered automatically by {@link HealthCheckService}.
 */
public interface HealthCheck {

  HealthCheckResult check();
}
