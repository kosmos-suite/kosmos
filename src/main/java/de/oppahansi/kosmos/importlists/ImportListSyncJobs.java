package de.oppahansi.kosmos.importlists;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.JobHandlerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * One {@link ImportListSyncJob} per enabled {@link ImportList} — see {@code JellyfinSyncJobs} for
 * the same pattern. Unlike Jellyfin sync, there's no dedicated "sync now" REST endpoint for a
 * single list — the generic {@code POST /jobs/import-list-sync-{id}/run} (backed by {@link
 * de.oppahansi.kosmos.scheduler.JobResource}) already covers that once the list shows up here.
 */
@ApplicationScoped
public class ImportListSyncJobs implements JobHandlerFactory {

  @Inject ImportListService importListService;

  @Override
  public List<JobHandler> currentHandlers() {
    return ImportList.<ImportList>list("enabled", true).stream()
        .<JobHandler>map(list -> new ImportListSyncJob(list.id, list.name, importListService))
        .toList();
  }
}
