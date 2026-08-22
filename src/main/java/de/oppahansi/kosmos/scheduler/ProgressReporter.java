package de.oppahansi.kosmos.scheduler;

/**
 * Reports live progress during a {@link JobHandler#run}, published by {@link JobRunner} as an SSE
 * event via {@link JobProgressBroadcaster} — never persisted. Call this as often as convenient; a
 * handler with nothing granular to report (most of them) simply never calls it.
 */
public interface ProgressReporter {

  void update(int current, int total, String message);
}
