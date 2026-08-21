package de.oppahansi.kosmos.scheduler;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One execution of a {@link ScheduledJob} — the history an Activity/Task-monitor page reads. */
@Entity
@Table(name = "job_run")
public class JobRun extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "scheduled_job_id")
  public ScheduledJob scheduledJob;

  @Column(name = "started_at", nullable = false)
  public Instant startedAt;

  @Column(name = "finished_at")
  public Instant finishedAt;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(length = 1000)
  public String message;
}
