package de.oppahansi.kosmos.scheduler;

import de.oppahansi.kosmos.scheduler.dto.JobProgressEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory pub/sub for live job progress — SSE, not polling or persistence. Progress is inherently
 * transient (meaningless once a run ends, never queried historically), so unlike the rest of a
 * job's state it never touches the database; {@link JobRunner} publishes directly here and {@link
 * JobResource} exposes {@link #subscribe} as an SSE stream.
 *
 * <p>The last event per job is cached and replayed to a new subscriber before live events, so a
 * client that opens a job's panel mid-run isn't blind until the next update happens to fire.
 */
@ApplicationScoped
public class JobProgressBroadcaster {

  private final Map<String, BroadcastProcessor<JobProgressEvent>> processors =
      new ConcurrentHashMap<>();
  private final Map<String, JobProgressEvent> lastEvent = new ConcurrentHashMap<>();

  public Multi<JobProgressEvent> subscribe(String jobName) {
    Multi<JobProgressEvent> live = processorFor(jobName);
    JobProgressEvent snapshot = lastEvent.get(jobName);
    return snapshot == null
        ? live
        : Multi.createBy().concatenating().streams(Multi.createFrom().item(snapshot), live);
  }

  public void publish(String jobName, JobProgressEvent event) {
    lastEvent.put(jobName, event);
    processorFor(jobName).onNext(event);
  }

  private BroadcastProcessor<JobProgressEvent> processorFor(String jobName) {
    return processors.computeIfAbsent(jobName, name -> BroadcastProcessor.create());
  }
}
