package de.oppahansi.kosmos.scheduler;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Config + last-run state for a recurring background task. See {@link JobHandler}. */
@Entity
@Table(name = "scheduled_job")
public class ScheduledJob extends KosmosEntity {

  @Column(nullable = false, unique = true, length = 100)
  public String name;

  @Column(name = "interval_seconds", nullable = false)
  public int intervalSeconds;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "last_run_at")
  public Instant lastRunAt;

  @Column(name = "last_status", length = 20)
  public String lastStatus;

  @Column(name = "last_message", length = 1000)
  public String lastMessage;

  public boolean isDue(Instant now) {
    return enabled && (lastRunAt == null || lastRunAt.plusSeconds(intervalSeconds).isBefore(now));
  }
}
