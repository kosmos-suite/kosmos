package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.ProgressReporter;
import java.util.UUID;

/**
 * Syncs one monitored {@link MovieCollection} — files a Request for each TMDB member not already
 * accounted for. Not a CDI bean — instances are built fresh by {@link MovieCollectionSyncJobs},
 * same reasoning as {@code ImportListSyncJob}.
 */
public class MovieCollectionSyncJob implements JobHandler {

  private final UUID collectionId;
  private final String collectionName;
  private final MovieCollectionService movieCollectionService;

  MovieCollectionSyncJob(
      UUID collectionId, String collectionName, MovieCollectionService movieCollectionService) {
    this.collectionId = collectionId;
    this.collectionName = collectionName;
    this.movieCollectionService = movieCollectionService;
  }

  @Override
  public String jobName() {
    return "movie-collection-sync-" + collectionId;
  }

  @Override
  public String displayName() {
    return "Collection Sync — " + collectionName;
  }

  @Override
  public int defaultIntervalSeconds() {
    return 21_600; // 6 hours
  }

  @Override
  public String run(ProgressReporter progress) {
    return movieCollectionService.sync(collectionId);
  }
}
