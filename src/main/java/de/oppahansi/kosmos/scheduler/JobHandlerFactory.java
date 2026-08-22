package de.oppahansi.kosmos.scheduler;

import java.util.List;

/**
 * Discovers a runtime-varying set of {@link JobHandler}s — for jobs scoped to a configurable entity
 * (e.g. one job per connected Jellyfin server) rather than a fixed, compile-time-known set.
 * Implementations are CDI beans discovered by {@link JobScheduler}, queried fresh on every tick so
 * adding or removing the underlying entity changes the job set without a restart.
 */
public interface JobHandlerFactory {

  List<JobHandler> currentHandlers();
}
