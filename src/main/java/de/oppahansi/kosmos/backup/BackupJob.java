package de.oppahansi.kosmos.backup;

import de.oppahansi.kosmos.scheduler.JobHandler;
import de.oppahansi.kosmos.scheduler.ProgressReporter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Registers the database backup as an ordinary scheduled job, so it gets the settings page's
 * existing enable/interval/"Run Now"/run-history UI for free rather than a bespoke one. Starts
 * disabled ({@link #autoScheduled}) — pg_dump availability and the backup directory both need a
 * user to confirm they work on this host (via "Run Now") before it's trusted to run unattended.
 */
@ApplicationScoped
public class BackupJob implements JobHandler {

  @Inject BackupService backupService;

  @Override
  public String jobName() {
    return "database-backup";
  }

  @Override
  public String displayName() {
    return "Database Backup";
  }

  @Override
  public int defaultIntervalSeconds() {
    return 86_400;
  }

  @Override
  public boolean autoScheduled() {
    return false;
  }

  @Override
  public String run(ProgressReporter progress) {
    return backupService.createBackup();
  }
}
