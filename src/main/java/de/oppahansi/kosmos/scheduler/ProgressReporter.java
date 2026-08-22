package de.oppahansi.kosmos.scheduler;

/**
 * Reports live progress during a {@link JobHandler#run}, polled by the settings UI off {@link
 * ScheduledJob#progressCurrent}/{@link ScheduledJob#progressTotal}/{@link
 * ScheduledJob#progressMessage}. {@link JobRunner} throttles the actual DB writes, so call this as
 * often as convenient — most calls are cheap no-ops. A handler with nothing granular to report
 * (most of them) simply never calls it.
 */
public interface ProgressReporter {

  void update(int current, int total, String message);
}
