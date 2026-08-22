package de.oppahansi.kosmos.downloads;

import static de.oppahansi.kosmos.downloads.HttpClients.MAPPER;
import static de.oppahansi.kosmos.downloads.HttpClients.REQUEST_TIMEOUT;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/** Thin client for qBittorrent's Web API (cookie-session auth, form-encoded requests). */
public class QbittorrentClient implements TorrentClient {

  /**
   * {@code error}: tracker/tracker-less failure past qBittorrent's own retry budget. {@code
   * missingFiles}: the download completed but its data is gone from disk. Every other state
   * (seeding, stalled, checking, queued, paused, ...) is a normal point in an eventually-successful
   * lifecycle, not a failure.
   */
  private static final Set<String> FAILURE_STATES = Set.of("error", "missingFiles");

  private final String baseUrl;
  private final HttpClient httpClient = HttpClients.withCookieJar();

  public QbittorrentClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  @Override
  public boolean login(String username, String password) throws IOException, InterruptedException {
    String form = formEncode("username", username) + "&" + formEncode("password", password);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/auth/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    // WebAPI v2.9+ (qBittorrent 5.x) returns 204 No Content on success; older versions returned
    // 200 with a literal "Ok." body (and, per the WebUI API docs, "Fails." on bad credentials —
    // still HTTP 200). Live-verified the 204 shape against a real 5.2.3 container on 2026-08-20;
    // the 200 body check below covers the older shape without needing another container to test
    // against.
    if (response.statusCode() == 204) {
      return true;
    }
    return response.statusCode() == 200 && !"Fails.".equals(response.body());
  }

  /** qBittorrent's {@code torrents/add} never returns a hash synchronously — always empty. */
  @Override
  public Optional<String> addTorrent(String url, Optional<String> category)
      throws IOException, InterruptedException {
    StringBuilder form = new StringBuilder(formEncode("urls", url));
    category.ifPresent(c -> form.append("&").append(formEncode("category", c)));

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return Optional.empty();
  }

  /**
   * The multipart-file variant of {@code /api/v2/torrents/add}, for a directly-uploaded {@code
   * .torrent} file rather than a URL {@link #addTorrent} can fetch.
   */
  @Override
  public Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException {
    MultipartFormBuilder form =
        new MultipartFormBuilder().file("torrents", filename, "application/x-bittorrent", content);
    category.ifPresent(c -> form.field("category", c));
    byte[] body = form.build();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/add"))
            .header("Content-Type", "multipart/form-data; boundary=" + form.boundary())
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return Optional.empty();
  }

  public String listTorrents() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/info"))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  @Override
  public Optional<TorrentStatus> getTorrentInfo(String hash)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/info?hashes=" + hash))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode results = MAPPER.readTree(response.body());
    if (!results.isArray() || results.isEmpty()) {
      return Optional.empty();
    }
    JsonNode torrent = results.get(0);
    String rawState = torrent.path("state").asText(null);
    double progress = torrent.path("progress").asDouble();
    boolean failed = FAILURE_STATES.contains(rawState);
    return Optional.of(
        new TorrentStatus(
            torrent.path("hash").asText(),
            failed ? DownloadState.FAILED : DownloadState.fromProgress(progress),
            progress,
            torrent.path("content_path").asText(null),
            failed ? "qBittorrent reported state: " + rawState : null));
  }

  public void deleteTorrent(String hash, boolean deleteFiles)
      throws IOException, InterruptedException {
    String form =
        formEncode("hashes", hash) + "&" + formEncode("deleteFiles", String.valueOf(deleteFiles));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/delete"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String formEncode(String key, String value) {
    return key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
