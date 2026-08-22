package de.oppahansi.kosmos.health.dto;

import de.oppahansi.kosmos.health.HealthCheckResult;

public record HealthCheckResponse(String source, String severity, String message) {

  public static HealthCheckResponse from(HealthCheckResult result) {
    return new HealthCheckResponse(result.source(), result.severity().name(), result.message());
  }
}
