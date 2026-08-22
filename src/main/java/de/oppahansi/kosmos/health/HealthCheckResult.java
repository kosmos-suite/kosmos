package de.oppahansi.kosmos.health;

/** The outcome of a single {@link HealthCheck}. */
public record HealthCheckResult(String source, Severity severity, String message) {

  public enum Severity {
    OK,
    WARNING,
    ERROR
  }

  public static HealthCheckResult ok(String source) {
    return new HealthCheckResult(source, Severity.OK, null);
  }

  public static HealthCheckResult warning(String source, String message) {
    return new HealthCheckResult(source, Severity.WARNING, message);
  }

  public static HealthCheckResult error(String source, String message) {
    return new HealthCheckResult(source, Severity.ERROR, message);
  }
}
