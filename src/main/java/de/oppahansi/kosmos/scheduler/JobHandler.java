package de.oppahansi.kosmos.scheduler;

/**
 * A recurring background task. Implementations are CDI beans discovered automatically by {@link
 * JobScheduler}.
 */
public interface JobHandler {

  /** Must match the {@link ScheduledJob#name} row this handler runs for. */
  String jobName();

  /** Used only to seed the {@link ScheduledJob} row the first time this handler is seen. */
  int defaultIntervalSeconds();

  /**
   * Throw to record a failed run — the message reaches {@link ScheduledJob#lastMessage} and the
   * {@link JobRun}.
   */
  void run();
}
