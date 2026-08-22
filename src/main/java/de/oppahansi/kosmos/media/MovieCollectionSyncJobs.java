package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.JobHandlerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * One {@link MovieCollectionSyncJob} per monitored {@link MovieCollection} — see {@code
 * ImportListSyncJobs} for the same pattern.
 */
@ApplicationScoped
public class MovieCollectionSyncJobs implements JobHandlerFactory {

  @Inject MovieCollectionService movieCollectionService;

  @Override
  public List<JobHandler> currentHandlers() {
    return MovieCollection.<MovieCollection>list("monitored", true).stream()
        .<JobHandler>map(
            collection ->
                new MovieCollectionSyncJob(collection.id, collection.name, movieCollectionService))
        .toList();
  }
}
