package de.oppahansi.kosmos.downloads;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared HTTP plumbing for the {@link TorrentClient} implementations — every one of them
 * (qBittorrent, Transmission, Deluge, SABnzbd, NZBGet) previously declared its own byte-identical
 * {@code ObjectMapper}/timeout constants and near-identical {@code HttpClient} construction.
 */
final class HttpClients {

  private HttpClients() {}

  static final ObjectMapper MAPPER = new ObjectMapper();
  static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
  static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

  /**
   * For clients whose auth is carried on every request (HTTP Basic, an API key query param, an RPC
   * session-id header) — no cookie jar needed.
   */
  static HttpClient basic() {
    return HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  /** For qBittorrent, whose auth is a cookie-based session a {@code login} call establishes. */
  static HttpClient withCookieJar() {
    return HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .cookieHandler(new CookieManager())
        .build();
  }
}
