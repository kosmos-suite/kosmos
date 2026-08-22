package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.jellyfin.dto.JellyfinSyncResult;
import de.oppahansi.kosmos.scheduler.JobHandler;
import java.util.UUID;

/**
 * Syncs one configured {@link JellyfinServer}. Not a CDI bean — the set of servers is
 * runtime-configurable, so instances are built fresh by {@link JellyfinSyncJobs} rather than
 * discovered as a fixed set like the download-search {@code JobHandler}s.
 */
public class JellyfinSyncJob implements JobHandler {

  private final UUID serverId;
  private final String serverName;
  private final JellyfinSyncService syncService;

  JellyfinSyncJob(UUID serverId, String serverName, JellyfinSyncService syncService) {
    this.serverId = serverId;
    this.serverName = serverName;
    this.syncService = syncService;
  }

  @Override
  public String jobName() {
    return "jellyfin-sync-" + serverId;
  }

  @Override
  public String displayName() {
    return "Jellyfin Sync — " + serverName;
  }

  @Override
  public int defaultIntervalSeconds() {
    return 1800; // 30 minutes
  }

  @Override
  public String run() {
    return summarize(syncService.sync(serverId));
  }

  static String summarize(JellyfinSyncResult result) {
    return ("Synced: %d movies added, %d already-owned linked, %d series added, %d episode files"
            + " linked, %d users added, %d users updated.")
        .formatted(
            result.created(),
            result.linked(),
            result.showsCreated(),
            result.episodeFilesLinked(),
            result.usersCreated(),
            result.usersUpdated());
  }
}
