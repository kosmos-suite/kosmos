package de.oppahansi.kosmos.scheduler.dto;

/**
 * One SSE frame on a job's live progress stream — never persisted, since progress is meaningless
 * once a run ends and isn't part of {@link de.oppahansi.kosmos.scheduler.JobRun} history. {@code
 * started} marks a claimed run before it's reported anything granular (covers handlers that never
 * call {@link de.oppahansi.kosmos.scheduler.ProgressReporter}); {@code progress} carries a
 * current/total/message triple; {@code finished} carries the run's outcome.
 */
public record JobProgressEvent(
    String kind, Integer current, Integer total, String message, String status) {

  public static JobProgressEvent started() {
    return new JobProgressEvent("started", null, null, null, null);
  }

  public static JobProgressEvent progress(int current, int total, String message) {
    return new JobProgressEvent("progress", current, total, message, null);
  }

  public static JobProgressEvent finished(String status, String message) {
    return new JobProgressEvent("finished", null, null, message, status);
  }
}
