package de.oppahansi.kosmos.downloads;

/**
 * A torrent/NZB's status, normalized across every download-client protocol's own vocabulary — each
 * {@link TorrentClient} implementation maps its own vendor status string into this before handing
 * it to {@link DownloadStatusPollJob}, which only ever needs to ask "done," "failed," or "still
 * going." Keeping that translation in each client (which already parses that vendor's payload)
 * rather than centralizing a table of every client's status strings here.
 */
public enum DownloadState {
  DOWNLOADING,
  COMPLETE,
  FAILED,
  UNKNOWN;

  /**
   * The fallback for a client whose raw status doesn't map to an explicit failure state —
   * completion is purely a function of progress once failure is ruled out.
   */
  public static DownloadState fromProgress(double progress) {
    return progress >= 1.0 ? COMPLETE : DOWNLOADING;
  }
}
