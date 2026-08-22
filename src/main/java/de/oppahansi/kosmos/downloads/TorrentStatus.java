package de.oppahansi.kosmos.downloads;

/**
 * A torrent's current state as reported by a download client, keyed by info-hash. {@code
 * failureReason} is only ever non-null when {@code state == FAILED} — the client's own raw status
 * text, kept for the {@link Blocklist} entry {@link DownloadStatusPollJob} creates from it.
 */
public record TorrentStatus(
    String hash, DownloadState state, double progress, String contentPath, String failureReason) {

  public boolean isComplete() {
    return state == DownloadState.COMPLETE;
  }

  public boolean isFailed() {
    return state == DownloadState.FAILED;
  }
}
