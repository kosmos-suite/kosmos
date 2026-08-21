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

/**
 * Thin client for SABnzbd's classic {@code /api} endpoint — the first genuinely different auth
 * model of the download clients so far: every request carries an {@code apikey} query parameter
 * instead of a session or Basic-auth header, so {@code username} is accepted for interface symmetry
 * but ignored (the API key is expected in {@code password}). Also the first Usenet client: {@link
 * #addTorrent}/{@link #addTorrentFile} return the real {@code nzo_id} synchronously (SABnzbd has no
 * magnet-URI-style embedded identifier for {@link GrabService} to fall back to otherwise), and
 * status has to be checked in two places — the queue while a job is still downloading, then history
 * once it's finished and post-processed, since SABnzbd moves a job between the two rather than
 * keeping one record with an evolving status.
 */
public class SabnzbdClient implements TorrentClient {

  private final String baseUrl;
  private final HttpClient httpClient = HttpClients.basic();
  private String apiKey;

  public SabnzbdClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  @Override
  public boolean login(String username, String password) throws IOException, InterruptedException {
    this.apiKey = password;
    JsonNode response = getJson("mode=version&output=json");
    return response.has("version") && !response.has("error");
  }

  @Override
  public Optional<String> addTorrent(String url, Optional<String> category)
      throws IOException, InterruptedException {
    String query =
        "mode=addurl&output=json&name="
            + URLEncoder.encode(url, StandardCharsets.UTF_8)
            + "&cat="
            + URLEncoder.encode(category.orElse(""), StandardCharsets.UTF_8);
    return firstNzoId(getJson(query));
  }

  @Override
  public Optional<String> addTorrentFile(byte[] content, String filename, Optional<String> category)
      throws IOException, InterruptedException {
    // SABnzbd's classic upload form field for the NZB file itself is (still) named "name".
    MultipartFormBuilder form =
        new MultipartFormBuilder()
            .field("apikey", apiKey)
            .field("mode", "addfile")
            .field("output", "json")
            .field("cat", category.orElse(""))
            .file("name", filename, "application/x-nzb", content);
    byte[] body = form.build();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api"))
            .header("Content-Type", "multipart/form-data; boundary=" + form.boundary())
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return firstNzoId(MAPPER.readTree(response.body()));
  }

  @Override
  public Optional<TorrentStatus> getTorrentInfo(String nzoId)
      throws IOException, InterruptedException {
    JsonNode queueSlots = getJson("mode=queue&output=json").path("queue").path("slots");
    for (JsonNode slot : queueSlots) {
      if (nzoId.equals(slot.path("nzo_id").asText())) {
        return Optional.of(
            new TorrentStatus(
                nzoId,
                slot.path("status").asText(null),
                slot.path("percentage").asDouble() / 100.0,
                null));
      }
    }

    JsonNode historySlots = getJson("mode=history&output=json").path("history").path("slots");
    for (JsonNode slot : historySlots) {
      if (nzoId.equals(slot.path("nzo_id").asText())) {
        boolean completed = "Completed".equals(slot.path("status").asText());
        return Optional.of(
            new TorrentStatus(
                nzoId,
                slot.path("status").asText(null),
                completed ? 1.0 : 0.0,
                completed ? slot.path("storage").asText(null) : null));
      }
    }

    return Optional.empty();
  }

  private Optional<String> firstNzoId(JsonNode response) {
    JsonNode nzoIds = response.path("nzo_ids");
    return nzoIds.isArray() && !nzoIds.isEmpty()
        ? Optional.of(nzoIds.get(0).asText())
        : Optional.empty();
  }

  private JsonNode getJson(String query) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api?" + query + "&apikey=" + apiKey))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return MAPPER.readTree(response.body());
  }
}
