package de.oppahansi.kosmos.jellyfin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thin client for the Jellyfin server API — never mutates anything server-side, token-header auth.
 */
public class JellyfinClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String baseUrl;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public JellyfinClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /**
   * All movie items the server has already scanned, with their TMDB id and on-disk path when known.
   */
  public List<JellyfinMovie> listMovies(String apiKey) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    baseUrl
                        + "/Items?IncludeItemTypes=Movie&Recursive=true&Fields=ProviderIds,Path"))
            .header("X-Emby-Token", apiKey)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode root = MAPPER.readTree(response.body());

    List<JellyfinMovie> movies = new ArrayList<>();
    for (JsonNode item : root.path("Items")) {
      String tmdbId = item.path("ProviderIds").path("Tmdb").asText(null);
      Integer year = item.hasNonNull("ProductionYear") ? item.path("ProductionYear").asInt() : null;
      movies.add(
          new JellyfinMovie(
              item.path("Name").asText(null), year, tmdbId, item.path("Path").asText(null)));
    }
    return movies;
  }

  /** Every user account the server knows about, with its admin flag. */
  public List<JellyfinUser> listUsers(String apiKey) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Users"))
            .header("X-Emby-Token", apiKey)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode root = MAPPER.readTree(response.body());

    List<JellyfinUser> users = new ArrayList<>();
    for (JsonNode item : root) {
      users.add(
          new JellyfinUser(
              item.path("Id").asText(null),
              item.path("Name").asText(null),
              item.path("Policy").path("IsAdministrator").asBoolean(false)));
    }
    return users;
  }

  /**
   * Verifies a username/password against this server via /Users/AuthenticateByName — the same call
   * the Jellyfin web client itself makes. Never stores or forwards the password anywhere else;
   * empty means the server rejected the credentials.
   */
  public Optional<JellyfinAuthResult> authenticate(String username, String password)
      throws IOException, InterruptedException {
    String body = "{\"Username\":\"" + escape(username) + "\",\"Pw\":\"" + escape(password) + "\"}";
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Users/AuthenticateByName"))
            .header("Content-Type", "application/json")
            .header(
                "X-Emby-Authorization",
                "MediaBrowser Client=\"Kosmos\", Device=\"Kosmos Server\", DeviceId=\"kosmos-server\", Version=\"0.1.0\"")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      return Optional.empty();
    }
    JsonNode user = MAPPER.readTree(response.body()).path("User");
    return Optional.of(
        new JellyfinAuthResult(
            user.path("Id").asText(null),
            user.path("Name").asText(null),
            user.path("Policy").path("IsAdministrator").asBoolean(false)));
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
