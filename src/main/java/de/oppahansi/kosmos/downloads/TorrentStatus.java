package de.oppahansi.kosmos.downloads;

/** A torrent's current state as reported by a download client, keyed by info-hash. */
public record TorrentStatus(String hash, String state, double progress, String contentPath) {

  public boolean isComplete() {
    return progress >= 1.0;
  }
}
