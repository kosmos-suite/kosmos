package de.oppahansi.kosmos.indexers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads Prowlarr's own configured indexer list. Prowlarr exposes each indexer it manages as its own
 * Torznab-compatible proxy endpoint ({@code {baseUrl}/{indexerId}/api}), the same way Sonarr/Radarr
 * consume it — so importing just means creating a Kosmos {@link Indexer} row per entry pointed at
 * that proxy URL; {@link TorznabClient} needs no Prowlarr-specific code.
 */
public class ProwlarrClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

  private final String baseUrl;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

  public ProwlarrClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  public List<ProwlarrIndexer> listIndexers(String apiKey)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/indexer"))
            .header("X-Api-Key", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Prowlarr returned " + response.statusCode());
    }
    JsonNode root = MAPPER.readTree(response.body());

    List<ProwlarrIndexer> indexers = new ArrayList<>();
    for (JsonNode item : root) {
      indexers.add(
          new ProwlarrIndexer(
              item.path("id").asInt(),
              item.path("name").asText(null),
              item.path("enable").asBoolean(false)));
    }
    return indexers;
  }

  public String torznabUrl(int prowlarrIndexerId) {
    return baseUrl + "/" + prowlarrIndexerId + "/api";
  }
}
