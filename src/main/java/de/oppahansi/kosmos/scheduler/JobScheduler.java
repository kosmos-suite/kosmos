package de.oppahansi.kosmos.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Ticks every {@link #TICK} and runs whichever {@link JobHandler}s are due, per {@link
 * ScheduledJob#intervalSeconds}.
 */
@ApplicationScoped
public class JobScheduler {

  static final String TICK = "30s";

  @Inject Instance<JobHandler> handlers;
  @Inject JobRunner jobRunner;

  @Scheduled(every = TICK)
  void tick() {
    for (JobHandler handler : handlers) {
      jobRunner.runIfDue(handler);
    }
  }
}
