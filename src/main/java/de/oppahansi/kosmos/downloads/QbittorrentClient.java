package de.oppahansi.kosmos.downloads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/** Thin client for qBittorrent's Web API (cookie-session auth, form-encoded requests). */
public class QbittorrentClient implements TorrentClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String baseUrl;
  private final HttpClient httpClient =
      HttpClient.newBuilder().cookieHandler(new java.net.CookieManager()).build();

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
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    // WebAPI v2.9+ (qBittorrent 5.x) returns 204 No Content on success; older versions returned
    // 200 with a literal "Ok." body. Live-verified against a real 5.2.3 container on 2026-08-20 —
    // checking only the legacy 200+"Ok." shape is what made every login attempt silently fail.
    return response.statusCode() == 200 || response.statusCode() == 204;
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
            .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return Optional.empty();
  }

  /**
   * The multipart-file variant of {@code /api/v2/torrents/add}, for a directly-uploaded {@code
   * .torrent} file rather than a URL {@link #addTorrent} can fetch. Hand-built rather than pulled
   * in via a multipart-client library, since this is the one place in the app that needs one.
   */
  @Override
  public Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException {
    String boundary = "KosmosBoundary" + UUID.randomUUID();
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writePart(body, boundary, "torrents", filename, "application/x-bittorrent", content);
    if (category.isPresent()) {
      writePart(
          body, boundary, "category", null, null, category.get().getBytes(StandardCharsets.UTF_8));
    }
    body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/add"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return Optional.empty();
  }

  private void writePart(
      ByteArrayOutputStream body,
      String boundary,
      String fieldName,
      String filename,
      String contentType,
      byte[] content)
      throws IOException {
    body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    String disposition =
        filename == null
            ? "Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n"
            : "Content-Disposition: form-data; name=\""
                + fieldName
                + "\"; filename=\""
                + filename
                + "\"\r\n";
    body.write(disposition.getBytes(StandardCharsets.UTF_8));
    if (contentType != null) {
      body.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
    body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    body.write(content);
    body.write("\r\n".getBytes(StandardCharsets.UTF_8));
  }

  public String listTorrents() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v2/torrents/info")).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  @Override
  public Optional<TorrentStatus> getTorrentInfo(String hash)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/info?hashes=" + hash))
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode results = MAPPER.readTree(response.body());
    if (!results.isArray() || results.isEmpty()) {
      return Optional.empty();
    }
    JsonNode torrent = results.get(0);
    return Optional.of(
        new TorrentStatus(
            torrent.path("hash").asText(),
            torrent.path("state").asText(null),
            torrent.path("progress").asDouble(),
            torrent.path("content_path").asText(null)));
  }

  public void deleteTorrent(String hash, boolean deleteFiles)
      throws IOException, InterruptedException {
    String form =
        formEncode("hashes", hash) + "&" + formEncode("deleteFiles", String.valueOf(deleteFiles));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v2/torrents/delete"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String formEncode(String key, String value) {
    return key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
