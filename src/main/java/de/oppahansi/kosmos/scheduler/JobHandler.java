package de.oppahansi.kosmos.scheduler;

/**
 * A recurring background task. Implementations are CDI beans discovered automatically by {@link
 * JobScheduler}.
 */
public interface JobHandler {

  /** Must match the {@link ScheduledJob#name} row this handler runs for. */
  String jobName();

  /** Human-readable label for the jobs settings page and failure notifications. */
  String displayName();

  /** Used only to seed the {@link ScheduledJob} row the first time this handler is seen. */
  int defaultIntervalSeconds();

  /**
   * Runs the job. The returned value (or {@code null}) becomes {@link ScheduledJob#lastMessage} and
   * the {@link JobRun#message} of a successful run. Throw to record a failed run instead — the
   * exception message reaches the same two fields.
   */
  String run();
}
