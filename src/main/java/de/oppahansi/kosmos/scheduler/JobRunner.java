package de.oppahansi.kosmos.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

/**
 * Executes a single {@link JobHandler} in its own transaction, so one job's failure never affects
 * another's bookkeeping.
 */
@ApplicationScoped
public class JobRunner {

  @Transactional
  public void runIfDue(JobHandler handler) {
    ScheduledJob job = findOrSeed(handler);
    if (!job.isDue(Instant.now())) {
      return;
    }

    Instant startedAt = Instant.now();
    JobRun jobRun = new JobRun();
    jobRun.scheduledJob = job;
    jobRun.startedAt = startedAt;

    try {
      handler.run();
      jobRun.status = "SUCCESS";
      job.lastStatus = "SUCCESS";
      job.lastMessage = null;
    } catch (RuntimeException e) {
      jobRun.status = "FAILED";
      jobRun.message = e.getMessage();
      job.lastStatus = "FAILED";
      job.lastMessage = e.getMessage();
    }

    jobRun.finishedAt = Instant.now();
    job.lastRunAt = startedAt;
    jobRun.persist();
  }

  private ScheduledJob findOrSeed(JobHandler handler) {
    Optional<ScheduledJob> existing =
        ScheduledJob.<ScheduledJob>find("name", handler.jobName()).firstResultOptional();
    if (existing.isPresent()) {
      return existing.get();
    }
    ScheduledJob job = new ScheduledJob();
    job.name = handler.jobName();
    job.intervalSeconds = handler.defaultIntervalSeconds();
    job.enabled = true;
    job.persist();
    return job;
  }
}
