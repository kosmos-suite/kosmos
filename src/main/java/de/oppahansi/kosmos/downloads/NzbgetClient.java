package de.oppahansi.kosmos.downloads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Thin client for NZBGet's JSON-RPC API — HTTP Basic auth per request, same as Transmission, unlike
 * SABnzbd's query-param API key. One real convenience the others don't have: {@code append}'s
 * {@code Content} parameter accepts either a raw NZB (base64-encoded) or a plain URL — NZBGet
 * detects which and fetches the URL itself asynchronously — so both {@link #addTorrent} and {@link
 * #addTorrentFile} are the same RPC call with a different {@code Content} shape. It returns the new
 * job's integer {@code NZBID} synchronously either way, the second Usenet client (after SABnzbd)
 * that doesn't need {@link GrabService}'s magnet-URI fallback.
 *
 * <p>Status, like SABnzbd, has to be checked in two places: {@code listgroups} for a job still in
 * the active queue, then {@code history} once it's finished (successfully or not) and left it.
 */
public class NzbgetClient implements TorrentClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

  private final String rpcUrl;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  private String authHeader;

  public NzbgetClient(String baseUrl) {
    String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.rpcUrl = trimmed + "/jsonrpc";
  }

  @Override
  public boolean login(String username, String password) throws IOException, InterruptedException {
    authHeader =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    JsonNode response = call("version", MAPPER.createArrayNode());
    return response.has("result") && !response.has("error");
  }

  @Override
  public Optional<String> addTorrent(String url, Optional<String> category)
      throws IOException, InterruptedException {
    String filename = url.substring(url.lastIndexOf('/') + 1);
    return append(filename.isBlank() ? "release.nzb" : filename, url, category);
  }

  @Override
  public Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException {
    return append(filename, Base64.getEncoder().encodeToString(content), category);
  }

  private Optional<String> append(String filename, String content, Optional<String> category)
      throws IOException, InterruptedException {
    ArrayNode params =
        MAPPER
            .createArrayNode()
            .add(filename)
            .add(content)
            .add(category.orElse(""))
            .add(0) // Priority
            .add(false) // AddToTop
            .add(false); // AddPaused
    params.add("").add(0).add("score"); // DupeKey, DupeScore, DupeMode
    params.add(MAPPER.createArrayNode()); // PPParameters

    JsonNode response = call("append", params);
    JsonNode result = response.path("result");
    return result.isInt() && result.asInt() > 0
        ? Optional.of(String.valueOf(result.asInt()))
        : Optional.empty();
  }

  @Override
  public Optional<TorrentStatus> getTorrentInfo(String id)
      throws IOException, InterruptedException {
    int nzbId = Integer.parseInt(id);

    JsonNode active = call("listgroups", MAPPER.createArrayNode().add(0)).path("result");
    for (JsonNode group : active) {
      if (group.path("NZBID").asInt() == nzbId) {
        double fileSizeMb = group.path("FileSizeMB").asDouble();
        double remainingMb = group.path("RemainingSizeMB").asDouble();
        double progress = fileSizeMb > 0 ? (fileSizeMb - remainingMb) / fileSizeMb : 0.0;
        return Optional.of(
            new TorrentStatus(id, group.path("Status").asText(null), progress, null));
      }
    }

    JsonNode history = call("history", MAPPER.createArrayNode().add(false)).path("result");
    for (JsonNode entry : history) {
      if (entry.path("NZBID").asInt() == nzbId) {
        String status = entry.path("Status").asText("");
        boolean success = status.startsWith("SUCCESS");
        String finalDir = entry.path("FinalDir").asText("");
        String destDir = entry.path("DestDir").asText("");
        String contentPath = success ? (finalDir.isBlank() ? destDir : finalDir) : null;
        return Optional.of(new TorrentStatus(id, status, success ? 1.0 : 0.0, contentPath));
      }
    }

    return Optional.empty();
  }

  private JsonNode call(String method, ArrayNode params) throws IOException, InterruptedException {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("method", method);
    body.set("params", params);
    body.put("id", 1);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(rpcUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", authHeader)
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return MAPPER.readTree(response.body());
  }
}
