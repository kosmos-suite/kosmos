package de.oppahansi.kosmos.importlists;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.ProgressReporter;
import java.util.UUID;

/**
 * Syncs one configured {@link ImportList}. Not a CDI bean — the set of lists is
 * runtime-configurable (same reasoning as {@code JellyfinLibrarySyncJob}), so instances are built
 * fresh by {@link ImportListSyncJobs} rather than discovered as a fixed set.
 */
public class ImportListSyncJob implements JobHandler {

  private final UUID listId;
  private final String listName;
  private final ImportListService importListService;

  ImportListSyncJob(UUID listId, String listName, ImportListService importListService) {
    this.listId = listId;
    this.listName = listName;
    this.importListService = importListService;
  }

  @Override
  public String jobName() {
    return "import-list-sync-" + listId;
  }

  @Override
  public String displayName() {
    return "Import List Sync — " + listName;
  }

  @Override
  public int defaultIntervalSeconds() {
    return 21_600; // 6 hours
  }

  @Override
  public String run(ProgressReporter progress) {
    return importListService.sync(listId);
  }
}
