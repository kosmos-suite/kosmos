package de.oppahansi.kosmos.downloads;

import static de.oppahansi.kosmos.downloads.HttpClients.MAPPER;
import static de.oppahansi.kosmos.downloads.HttpClients.REQUEST_TIMEOUT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Thin client for Transmission's RPC API (transmissionrpc.readthedocs.io/en/latest/rpc-spec.html).
 * Two protocol differences from {@link QbittorrentClient} worth calling out: there's no session
 * login endpoint — every request carries HTTP Basic auth instead, so {@link #login} just probes
 * with a cheap {@code session-get} call rather than establishing a cookie; and every request must
 * carry an {@code X-Transmission-Session-Id} header that the server hands out via a 409 response to
 * the first request, so a stale/missing id is refreshed transparently on that 409 rather than
 * treated as a failure.
 */
public class TransmissionClient implements TorrentClient {

  private final String rpcUrl;
  private final HttpClient httpClient = HttpClients.basic();
  private String authHeader;
  private volatile String sessionId = "";

  public TransmissionClient(String baseUrl) {
    String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.rpcUrl = trimmed + "/transmission/rpc";
  }

  @Override
  public boolean login(String username, String password) throws IOException, InterruptedException {
    authHeader =
        username == null || username.isBlank()
            ? null
            : "Basic "
                + Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    JsonNode response = call("session-get", MAPPER.createObjectNode());
    return "success".equals(response.path("result").asText());
  }

  @Override
  public Optional<String> addTorrent(String url, Optional<String> category)
      throws IOException, InterruptedException {
    ObjectNode args = MAPPER.createObjectNode();
    args.put("filename", url);
    category.ifPresent(c -> args.putArray("labels").add(c));
    call("torrent-add", args);
    return Optional.empty();
  }

  @Override
  public Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException {
    ObjectNode args = MAPPER.createObjectNode();
    args.put("metainfo", Base64.getEncoder().encodeToString(content));
    category.ifPresent(c -> args.putArray("labels").add(c));
    call("torrent-add", args);
    return Optional.empty();
  }

  /**
   * Transmission has no single "content path" field — {@code downloadDir} + {@code name}
   * reconstructs it the same way its own clients do. {@code percentDone} is already 0.0–1.0, the
   * same scale {@link TorrentStatus#isComplete()} expects.
   */
  @Override
  public Optional<TorrentStatus> getTorrentInfo(String hash)
      throws IOException, InterruptedException {
    ObjectNode args = MAPPER.createObjectNode();
    args.putArray("ids").add(hash);
    args.putArray("fields")
        .add("hashString")
        .add("status")
        .add("percentDone")
        .add("downloadDir")
        .add("name");
    JsonNode torrents = call("torrent-get", args).path("arguments").path("torrents");
    if (!torrents.isArray() || torrents.isEmpty()) {
      return Optional.empty();
    }
    JsonNode torrent = torrents.get(0);
    String downloadDir = torrent.path("downloadDir").asText("");
    String name = torrent.path("name").asText("");
    String contentPath = downloadDir.isBlank() ? null : downloadDir + "/" + name;
    return Optional.of(
        new TorrentStatus(
            torrent.path("hashString").asText(),
            torrent.path("status").asText(null),
            torrent.path("percentDone").asDouble(),
            contentPath));
  }

  private JsonNode call(String method, ObjectNode arguments)
      throws IOException, InterruptedException {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("method", method);
    body.set("arguments", arguments);

    HttpResponse<String> response = send(body);
    if (response.statusCode() == 409) {
      sessionId = response.headers().firstValue("X-Transmission-Session-Id").orElse("");
      response = send(body);
    }
    return MAPPER.readTree(response.body());
  }

  private HttpResponse<String> send(ObjectNode body) throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(rpcUrl))
            .header("Content-Type", "application/json")
            .header("X-Transmission-Session-Id", sessionId)
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
    if (authHeader != null) {
      builder.header("Authorization", authHeader);
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }
}
