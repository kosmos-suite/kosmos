package de.oppahansi.kosmos.scheduler;

import de.oppahansi.kosmos.notifications.NotificationService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Executes a single {@link JobHandler}. Claiming a job ({@link #claim}: checking it's due/not
 * already running, and marking it running) and recording its outcome ({@link #finish}) are each
 * their own committed transaction, with the handler's own {@link JobHandler#run()} in between
 * outside any transaction of this class's own — unlike the previous single-transaction design, this
 * means a concurrent {@link JobScheduler#tick()} sees "already running" the instant a job is
 * claimed, not only after the whole run (which can include the handler's own slow external HTTP
 * calls) finally commits.
 */
@ApplicationScoped
public class JobRunner {

  @Inject NotificationService notificationService;

  /** Runs {@code handler} if it's due and not already running — a no-op otherwise. */
  public void runIfDue(JobHandler handler) {
    run(handler, false);
  }

  /**
   * Runs {@code handler} right now regardless of its schedule, unless it's already mid-run. Returns
   * the resulting {@link JobRun}, or empty if it was already running.
   */
  public Optional<JobRun> runNow(JobHandler handler) {
    return Optional.ofNullable(run(handler, true));
  }

  private JobRun run(JobHandler handler, boolean force) {
    ScheduledJob job = QuarkusTransaction.requiringNew().call(() -> claim(handler, force));
    if (job == null) {
      return null;
    }

    Instant startedAt = Instant.now();
    String status;
    String message;
    try {
      message = handler.run();
      status = "SUCCESS";
    } catch (RuntimeException e) {
      status = "FAILED";
      message = e.getMessage();
    }

    UUID jobId = job.id;
    String finalStatus = status;
    String finalMessage = message;
    JobRun jobRun =
        QuarkusTransaction.requiringNew()
            .call(() -> finish(jobId, startedAt, finalStatus, finalMessage));

    if ("FAILED".equals(status)) {
      notificationService.notifyAll(
          "Job failed",
          handler.displayName() + " failed" + (message != null ? ": " + message : "."));
    }
    return jobRun;
  }

  private ScheduledJob claim(JobHandler handler, boolean force) {
    ScheduledJob job = findOrSeed(handler);
    boolean runnable = force ? job.runningSince == null : job.isDue(Instant.now());
    if (!runnable) {
      return null;
    }
    job.runningSince = Instant.now();
    return job;
  }

  private JobRun finish(UUID jobId, Instant startedAt, String status, String message) {
    ScheduledJob job = ScheduledJob.<ScheduledJob>findById(jobId);
    job.runningSince = null;
    job.lastRunAt = startedAt;
    job.lastStatus = status;
    job.lastMessage = message;

    JobRun jobRun = new JobRun();
    jobRun.scheduledJob = job;
    jobRun.startedAt = startedAt;
    jobRun.finishedAt = Instant.now();
    jobRun.status = status;
    jobRun.message = message;
    jobRun.persist();
    return jobRun;
  }

  private ScheduledJob findOrSeed(JobHandler handler) {
    Optional<ScheduledJob> existing =
        ScheduledJob.<ScheduledJob>find("name", handler.jobName()).firstResultOptional();
    if (existing.isPresent()) {
      ScheduledJob job = existing.get();
      // Code-owned, unlike intervalSeconds/enabled which the user can edit — always kept in sync
      // so a handler rename doesn't leave a stale label in the DB.
      job.displayName = handler.displayName();
      return job;
    }
    ScheduledJob job = new ScheduledJob();
    job.name = handler.jobName();
    job.displayName = handler.displayName();
    job.intervalSeconds = handler.defaultIntervalSeconds();
    job.enabled = true;
    job.persist();
    return job;
  }
}
