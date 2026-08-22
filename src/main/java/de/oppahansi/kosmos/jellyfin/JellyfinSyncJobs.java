package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.JobHandlerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link JellyfinLibrarySyncJob} and a {@link JellyfinUserImportJob} per enabled {@link
 * JellyfinServer} — mirrors Seerr's one job row per connected media-server integration, split the
 * same way Seerr splits "Radarr Scan" from other per-integration tasks rather than one job doing
 * everything. A disabled server drops out of the recurring schedule but can still be run manually
 * via {@link #libraryJobForServer}/{@link #userJobForServer}, matching {@code
 * JellyfinServerResource}'s sync endpoints not gating on {@link JellyfinServer#enabled} today.
 */
@ApplicationScoped
public class JellyfinSyncJobs implements JobHandlerFactory {

  @Inject JellyfinSyncService syncService;

  @Override
  public List<JobHandler> currentHandlers() {
    return JellyfinServer.<JellyfinServer>list("enabled", true).stream()
        .flatMap(server -> jobsFor(server).stream())
        .toList();
  }

  public Optional<JobHandler> libraryJobForServer(UUID serverId) {
    return JellyfinServer.<JellyfinServer>findByIdOptional(serverId)
        .map(server -> new JellyfinLibrarySyncJob(server.id, server.name, syncService));
  }

  public Optional<JobHandler> userJobForServer(UUID serverId) {
    return JellyfinServer.<JellyfinServer>findByIdOptional(serverId)
        .map(server -> new JellyfinUserImportJob(server.id, server.name, syncService));
  }

  private List<JobHandler> jobsFor(JellyfinServer server) {
    return List.of(
        new JellyfinLibrarySyncJob(server.id, server.name, syncService),
        new JellyfinUserImportJob(server.id, server.name, syncService));
  }
}
