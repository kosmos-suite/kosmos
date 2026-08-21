package de.oppahansi.kosmos.scheduler.dto;

import de.oppahansi.kosmos.scheduler.ScheduledJob;
import java.time.Instant;
import java.util.UUID;

public record ScheduledJobResponse(
    UUID id,
    String name,
    int intervalSeconds,
    boolean enabled,
    Instant lastRunAt,
    String lastStatus,
    String lastMessage) {

  public static ScheduledJobResponse from(ScheduledJob job) {
    return new ScheduledJobResponse(
        job.id,
        job.name,
        job.intervalSeconds,
        job.enabled,
        job.lastRunAt,
        job.lastStatus,
        job.lastMessage);
  }
}
