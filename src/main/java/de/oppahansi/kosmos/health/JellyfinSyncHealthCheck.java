package de.oppahansi.kosmos.health;

import de.oppahansi.kosmos.jellyfin.JellyfinServer;
import de.oppahansi.kosmos.jellyfin.JellyfinServerService;
import de.oppahansi.kosmos.scheduler.ScheduledJob;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Flags any enabled {@link JellyfinServer} whose library-sync job's most recent run failed. A
 * server that has never synced yet (no {@link ScheduledJob} row seeded for it) isn't flagged — that
 * just means it hasn't had its first tick, not that something's broken.
 */
@ApplicationScoped
public class JellyfinSyncHealthCheck implements HealthCheck {

  @Inject JellyfinServerService jellyfinServerService;

  @Override
  public HealthCheckResult check() {
    List<String> failing =
        jellyfinServerService.listAll().stream()
            .filter(server -> server.enabled)
            .filter(this::lastSyncFailed)
            .map(server -> server.name)
            .toList();
    if (failing.isEmpty()) {
      return HealthCheckResult.ok("Jellyfin Sync");
    }
    return HealthCheckResult.error(
        "Jellyfin Sync", "Last sync failed for: " + String.join(", ", failing) + ".");
  }

  private boolean lastSyncFailed(JellyfinServer server) {
    return ScheduledJob.<ScheduledJob>find("name", "jellyfin-library-sync-" + server.id)
        .firstResultOptional()
        .map(job -> "FAILED".equals(job.lastStatus))
        .orElse(false);
  }
}
