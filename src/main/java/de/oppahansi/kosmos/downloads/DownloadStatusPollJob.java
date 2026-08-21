package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.library.ImportService;
import de.oppahansi.kosmos.scheduler.JobHandler;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Polls each in-flight {@link Grab} against its download client; once it completes, hands its
 * content path to {@link ImportService} and marks the Grab imported. This is what actually makes
 * {@link Grab#status} reach anything past {@code GRABBED} — see the field's own comment. Only Grabs
 * with a known {@code jobId} are pollable (see {@link GrabService} for which grabs get one), and
 * the download client must see the same filesystem paths Kosmos does — same constraint every *arr
 * app has, no path-remapping support yet.
 */
@ApplicationScoped
public class DownloadStatusPollJob implements JobHandler {

  @Inject ImportService importService;

  @Override
  public String jobName() {
    return "download-status-poll";
  }

  @Override
  public int defaultIntervalSeconds() {
    return 30;
  }

  /**
   * Each Grab gets its own fresh transaction (rather than one for the whole batch) so one grab's
   * failure — an unreachable client, a re-imported path — can't roll back another grab's
   * already-successful status update.
   */
  @Override
  public void run() {
    List<Grab> pending =
        QuarkusTransaction.requiringNew()
            .call(() -> Grab.list("status = ?1 and jobId is not null", "GRABBED"));
    for (Grab grab : pending) {
      // Caught here, not just inside pollOne: an unimportable file (sample-sized, duplicate
      // path) is a normal per-grab outcome, not a job malfunction — it must never abort
      // processing of the other pending grabs in this same run, nor mark the whole job FAILED.
      try {
        QuarkusTransaction.requiringNew().run(() -> pollOne(grab.id));
      } catch (RuntimeException e) {
        // Left GRABBED; retried on the next tick.
      }
    }
  }

  private void pollOne(UUID grabId) {
    Grab grab = Grab.<Grab>findById(grabId);
    DownloadClient client = grab.downloadClient;
    TorrentClient torrentClient = TorrentClients.forConfig(client);
    Optional<TorrentStatus> status;
    try {
      if (!torrentClient.login(client.username, client.password)) {
        return;
      }
      status = torrentClient.getTorrentInfo(grab.jobId);
    } catch (IOException | InterruptedException e) {
      return;
    }

    if (status.isEmpty() || !status.get().isComplete()) {
      return;
    }

    String contentPath = status.get().contentPath();
    if (contentPath == null || contentPath.isBlank()) {
      return;
    }

    importService.importPath(grab.release.mediaItem, contentPath);
    grab.status = "IMPORTED";
  }
}
