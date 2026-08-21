package de.oppahansi.kosmos.downloads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Optional;

/**
 * Thin client for Deluge's JSON-RPC WebUI API. One real protocol quirk the others don't have: the
 * WebUI is a proxy in front of a separate {@code deluged} daemon process, and starts each session
 * disconnected from it — {@code core.*} methods 404-equivalent (empty/null results) until {@code
 * web.connect} is called against one of the hosts {@code web.get_hosts} reports. A self-hosted
 * install has its daemon co-located and pre-registered, so connecting to the first (only) host is
 * the real-world case, not a shortcut.
 *
 * <p>Login takes a password only, no username — {@code username} is accepted for interface symmetry
 * with the other clients but ignored. Deluge's own {@code progress} is 0–100, not 0.0–1.0 like
 * qBittorrent/Transmission, so it's normalized on the way into {@link TorrentStatus}.
 */
public class DelugeClient implements TorrentClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String rpcUrl;
  private final HttpClient httpClient =
      HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
  private int nextId = 1;

  public DelugeClient(String baseUrl) {
    String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.rpcUrl = trimmed + "/json";
  }

  @Override
  public boolean login(String username, String password) throws IOException, InterruptedException {
    JsonNode loginResult = call("auth.login", MAPPER.createArrayNode().add(password));
    if (!loginResult.path("result").asBoolean(false)) {
      return false;
    }
    JsonNode hosts = call("web.get_hosts", MAPPER.createArrayNode()).path("result");
    if (!hosts.isArray() || hosts.isEmpty()) {
      return false;
    }
    String hostId = hosts.get(0).get(0).asText();
    JsonNode connectResult = call("web.connect", MAPPER.createArrayNode().add(hostId));
    return connectResult.path("error").isNull();
  }

  /**
   * Unlike qBittorrent, Deluge's {@code add_torrent_*} calls return the info-hash synchronously.
   */
  @Override
  public Optional<String> addTorrent(String url, Optional<String> category)
      throws IOException, InterruptedException {
    String method = url.startsWith("magnet:") ? "core.add_torrent_magnet" : "core.add_torrent_url";
    ArrayNode params = MAPPER.createArrayNode().add(url);
    params.add(MAPPER.createObjectNode());
    return resultHash(call(method, params));
  }

  @Override
  public Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException {
    ArrayNode params =
        MAPPER.createArrayNode().add(filename).add(Base64.getEncoder().encodeToString(content));
    params.add(MAPPER.createObjectNode());
    return resultHash(call("core.add_torrent_file", params));
  }

  private Optional<String> resultHash(JsonNode response) {
    JsonNode result = response.path("result");
    return result.isTextual() ? Optional.of(result.asText()) : Optional.empty();
  }

  @Override
  public Optional<TorrentStatus> getTorrentInfo(String hash)
      throws IOException, InterruptedException {
    ArrayNode fields =
        MAPPER.createArrayNode().add("progress").add("state").add("save_path").add("name");
    JsonNode result =
        call("core.get_torrent_status", MAPPER.createArrayNode().add(hash).add(fields))
            .path("result");
    if (result.isMissingNode() || result.isNull() || !result.has("name")) {
      return Optional.empty();
    }
    String savePath = result.path("save_path").asText("");
    String name = result.path("name").asText("");
    String contentPath = savePath.isBlank() ? null : savePath + "/" + name;
    return Optional.of(
        new TorrentStatus(
            hash,
            result.path("state").asText(null),
            result.path("progress").asDouble() / 100.0,
            contentPath));
  }

  private JsonNode call(String method, ArrayNode params) throws IOException, InterruptedException {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("method", method);
    body.set("params", params);
    body.put("id", nextId++);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(rpcUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return MAPPER.readTree(response.body());
  }
}
