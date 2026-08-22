package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.JobHandlerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One {@link JellyfinSyncJob} per enabled {@link JellyfinServer} — mirrors Seerr's one job row per
 * connected media-server integration. A disabled server drops out of the recurring schedule but can
 * still be synced manually via {@link #forServer}, matching {@code JellyfinServerResource#sync} not
 * gating on {@link JellyfinServer#enabled} today.
 */
@ApplicationScoped
public class JellyfinSyncJobs implements JobHandlerFactory {

  @Inject JellyfinSyncService syncService;

  @Override
  public List<JobHandler> currentHandlers() {
    return JellyfinServer.<JellyfinServer>list("enabled", true).stream()
        .map(server -> (JobHandler) new JellyfinSyncJob(server.id, server.name, syncService))
        .toList();
  }

  public Optional<JobHandler> forServer(UUID serverId) {
    return JellyfinServer.<JellyfinServer>findByIdOptional(serverId)
        .map(server -> new JellyfinSyncJob(server.id, server.name, syncService));
  }
}
