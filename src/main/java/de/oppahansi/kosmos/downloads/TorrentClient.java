package de.oppahansi.kosmos.downloads;

import java.io.IOException;
import java.util.Optional;

/**
 * Protocol-level operations Kosmos needs from a download client, implemented once per {@link
 * DownloadClient#type} (see {@link TorrentClients#forConfig}). Deliberately shaped after what
 * {@link GrabService}/{@link DownloadStatusPollJob} actually call — not a speculative full client
 * API surface. Despite the name, this covers Usenet clients too (SABnzbd/NZBGet) — an NZB add is
 * structurally the same shape as a torrent add (a URL or a file, an optional category, and an
 * opaque id to poll status by later), just without a magnet-URI-style embedded identifier.
 */
public interface TorrentClient {

  /**
   * Authenticates (or, for a client whose protocol authenticates per-request instead of via a
   * session, verifies the given credentials work) before any other call.
   */
  boolean login(String username, String password) throws IOException, InterruptedException;

  /**
   * The id to poll status by, if the client can determine one synchronously from this call alone —
   * some can't (qBittorrent's {@code torrents/add} returns no hash), in which case {@link
   * GrabService} falls back to whatever it can parse out of the release's own download URL (a
   * magnet URI's embedded info-hash) instead.
   */
  Optional<String> addTorrent(String url, Optional<String> category)
      throws IOException, InterruptedException;

  Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException;

  Optional<TorrentStatus> getTorrentInfo(String id) throws IOException, InterruptedException;
}
