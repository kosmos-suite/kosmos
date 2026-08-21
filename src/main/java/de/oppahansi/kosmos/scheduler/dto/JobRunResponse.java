package de.oppahansi.kosmos.scheduler.dto;

import de.oppahansi.kosmos.scheduler.JobRun;
import java.time.Instant;
import java.util.UUID;

/** One row of a {@link de.oppahansi.kosmos.scheduler.ScheduledJob}'s run history. */
public record JobRunResponse(
    UUID id, Instant startedAt, Instant finishedAt, String status, String message) {

  public static JobRunResponse from(JobRun run) {
    return new JobRunResponse(run.id, run.startedAt, run.finishedAt, run.status, run.message);
  }
}
