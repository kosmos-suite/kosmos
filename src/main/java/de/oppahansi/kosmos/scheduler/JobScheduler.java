package de.oppahansi.kosmos.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * Ticks every {@link #TICK} and runs whichever {@link JobHandler}s are due, per {@link
 * ScheduledJob#intervalSeconds}. Also the lookup {@link JobResource} uses to find the handler
 * behind a "run now"/enable/interval-edit request by {@link ScheduledJob#name}.
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

  public Optional<JobHandler> findByName(String jobName) {
    return handlers.stream().filter(h -> h.jobName().equals(jobName)).findFirst();
  }
}
