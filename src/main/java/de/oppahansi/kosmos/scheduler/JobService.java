package de.oppahansi.kosmos.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Backs {@link JobResource} — the read/write side of job monitoring, distinct from {@link
 * JobRunner}'s execution.
 */
@ApplicationScoped
public class JobService {

  private static final int DEFAULT_RUN_HISTORY_LIMIT = 50;

  @Inject JobScheduler jobScheduler;
  @Inject JobRunner jobRunner;

  public List<ScheduledJob> listAll() {
    return ScheduledJob.listAll();
  }

  public Optional<ScheduledJob> findByName(String name) {
    return ScheduledJob.<ScheduledJob>find("name", name).firstResultOptional();
  }

  /**
   * Most recent runs first, capped at {@link #DEFAULT_RUN_HISTORY_LIMIT}. Empty if the job has
   * never run.
   */
  public List<JobRun> runsFor(String name, int limit) {
    return findByName(name)
        .map(
            job ->
                JobRun.<JobRun>find("scheduledJob = ?1 order by startedAt desc", job)
                    .page(0, Math.min(limit, DEFAULT_RUN_HISTORY_LIMIT))
                    .list())
        .orElse(List.of());
  }

  /**
   * The {@link JobHandler} registered for {@code name}, if any — {@link JobResource} 404s when it
   * isn't.
   */
  public boolean handlerExists(String name) {
    return jobScheduler.findByName(name).isPresent();
  }

  /**
   * Runs the job now. Empty means it was already mid-run, which {@link JobResource} reports as 409
   * — call {@link #handlerExists} first to tell that apart from "no such job" (404).
   */
  public Optional<JobRun> runNow(String name) {
    return jobScheduler.findByName(name).flatMap(jobRunner::runNow);
  }

  @Transactional
  public Optional<ScheduledJob> update(String name, boolean enabled, int intervalSeconds) {
    return findByName(name)
        .map(
            job -> {
              job.enabled = enabled;
              job.intervalSeconds = intervalSeconds;
              return job;
            });
  }
}
