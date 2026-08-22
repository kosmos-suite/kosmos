package de.oppahansi.kosmos.health;

import de.oppahansi.kosmos.downloads.DownloadClient;
import jakarta.enterprise.context.ApplicationScoped;

/** A grabbed release has nowhere to be sent without at least one enabled download client. */
@ApplicationScoped
public class DownloadClientHealthCheck implements HealthCheck {

  @Override
  public HealthCheckResult check() {
    long enabled = DownloadClient.count("enabled", true);
    if (enabled == 0) {
      return HealthCheckResult.error(
          "Download Clients", "No enabled download client — a grab has nowhere to be sent.");
    }
    return HealthCheckResult.ok("Download Clients");
  }
}
