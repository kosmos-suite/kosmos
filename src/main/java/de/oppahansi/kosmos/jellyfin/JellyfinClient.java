package de.oppahansi.kosmos.jellyfin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin client for the Jellyfin server API — never mutates anything server-side, token-header auth.
 */
public class JellyfinClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

  private final String baseUrl;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

  public JellyfinClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /**
   * All movie items the server has already scanned, with their TMDB id and on-disk path when known.
   */
  public List<JellyfinMovie> listMovies(String apiKey) throws IOException, InterruptedException {
    return listMoviesUnder(apiKey, null);
  }

  /**
   * Same as {@link #listMovies(String)}, restricted to the given library (Jellyfin ItemId) ids — or
   * every library when {@code libraryIds} is null/empty.
   */
  public List<JellyfinMovie> listMovies(String apiKey, List<String> libraryIds)
      throws IOException, InterruptedException {
    if (libraryIds == null || libraryIds.isEmpty()) {
      return listMoviesUnder(apiKey, null);
    }
    List<JellyfinMovie> movies = new ArrayList<>();
    for (String libraryId : libraryIds) {
      movies.addAll(listMoviesUnder(apiKey, libraryId));
    }
    return movies;
  }

  private List<JellyfinMovie> listMoviesUnder(String apiKey, String parentId)
      throws IOException, InterruptedException {
    String parentParam = parentId == null ? "" : "&ParentId=" + parentId;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    baseUrl
                        + "/Items?IncludeItemTypes=Movie&Recursive=true&Fields=ProviderIds,Path"
                        + parentParam))
            .header("X-Emby-Token", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    checkOk(response, "list movies");
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

  /**
   * All series items the server has already scanned, with their TMDB id and root folder path when
   * known. Restricted to the given library ids, or every library when null/empty — same shape as
   * {@link #listMovies(String, List)}.
   */
  public List<JellyfinShow> listShows(String apiKey, List<String> libraryIds)
      throws IOException, InterruptedException {
    if (libraryIds == null || libraryIds.isEmpty()) {
      return listShowsUnder(apiKey, null);
    }
    List<JellyfinShow> shows = new ArrayList<>();
    for (String libraryId : libraryIds) {
      shows.addAll(listShowsUnder(apiKey, libraryId));
    }
    return shows;
  }

  private List<JellyfinShow> listShowsUnder(String apiKey, String parentId)
      throws IOException, InterruptedException {
    String parentParam = parentId == null ? "" : "&ParentId=" + parentId;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    baseUrl
                        + "/Items?IncludeItemTypes=Series&Recursive=true&Fields=ProviderIds,Path"
                        + parentParam))
            .header("X-Emby-Token", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    checkOk(response, "list shows");
    JsonNode root = MAPPER.readTree(response.body());

    List<JellyfinShow> shows = new ArrayList<>();
    for (JsonNode item : root.path("Items")) {
      String tmdbId = item.path("ProviderIds").path("Tmdb").asText(null);
      Integer year = item.hasNonNull("ProductionYear") ? item.path("ProductionYear").asInt() : null;
      shows.add(
          new JellyfinShow(
              item.path("Id").asText(null),
              item.path("Name").asText(null),
              year,
              tmdbId,
              item.path("Path").asText(null)));
    }
    return shows;
  }

  /**
   * All episode items the server has already scanned, with the season/episode number and path
   * needed to match each file against Kosmos's own TMDB-built episode tree — see {@link
   * JellyfinEpisode}.
   */
  public List<JellyfinEpisode> listEpisodes(String apiKey, List<String> libraryIds)
      throws IOException, InterruptedException {
    if (libraryIds == null || libraryIds.isEmpty()) {
      return listEpisodesUnder(apiKey, null);
    }
    List<JellyfinEpisode> episodes = new ArrayList<>();
    for (String libraryId : libraryIds) {
      episodes.addAll(listEpisodesUnder(apiKey, libraryId));
    }
    return episodes;
  }

  private List<JellyfinEpisode> listEpisodesUnder(String apiKey, String parentId)
      throws IOException, InterruptedException {
    String parentParam = parentId == null ? "" : "&ParentId=" + parentId;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    baseUrl
                        + "/Items?IncludeItemTypes=Episode&Recursive=true&Fields=Path,ParentIndexNumber,IndexNumber,SeriesId"
                        + parentParam))
            .header("X-Emby-Token", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    checkOk(response, "list episodes");
    JsonNode root = MAPPER.readTree(response.body());

    List<JellyfinEpisode> episodes = new ArrayList<>();
    for (JsonNode item : root.path("Items")) {
      Integer season =
          item.hasNonNull("ParentIndexNumber") ? item.path("ParentIndexNumber").asInt() : null;
      Integer episodeNumber =
          item.hasNonNull("IndexNumber") ? item.path("IndexNumber").asInt() : null;
      episodes.add(
          new JellyfinEpisode(
              item.path("SeriesId").asText(null),
              season,
              episodeNumber,
              item.path("Path").asText(null)));
    }
    return episodes;
  }

  /** Top-level library folders (Movies, TV Shows, ...) for picking which ones to sync. */
  public List<JellyfinLibrary> listLibraries(String apiKey)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Library/VirtualFolders"))
            .header("X-Emby-Token", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    checkOk(response, "list libraries");
    JsonNode root = MAPPER.readTree(response.body());

    List<JellyfinLibrary> libraries = new ArrayList<>();
    for (JsonNode item : root) {
      List<String> locations = new ArrayList<>();
      for (JsonNode location : item.path("Locations")) {
        locations.add(location.asText());
      }
      libraries.add(
          new JellyfinLibrary(
              item.path("ItemId").asText(null),
              item.path("Name").asText(null),
              item.path("CollectionType").asText(null),
              locations));
    }
    return libraries;
  }

  /** Every user account the server knows about, with its admin flag. */
  public List<JellyfinUser> listUsers(String apiKey) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Users"))
            .header("X-Emby-Token", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    checkOk(response, "list users");
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
   * empty means the server rejected the credentials. Jellyfin ties sessions to (user, device); a
   * fresh random device id per call keeps each login's session independent of any other.
   */
  public Optional<JellyfinAuthResult> authenticate(String username, String password)
      throws IOException, InterruptedException {
    return authenticate(username, password, "kosmos-login-" + UUID.randomUUID());
  }

  /**
   * Same as {@link #authenticate(String, String)}, with an explicit device id. Reusing a device id
   * across two authenticate() calls invalidates the first call's session token.
   */
  public Optional<JellyfinAuthResult> authenticate(
      String username, String password, String deviceId) throws IOException, InterruptedException {
    String body = "{\"Username\":\"" + escape(username) + "\",\"Pw\":\"" + escape(password) + "\"}";
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/Users/AuthenticateByName"))
            .header("Content-Type", "application/json")
            .header(
                "X-Emby-Authorization",
                "MediaBrowser Client=\"Kosmos\", Device=\"Kosmos Server\", DeviceId=\"%s\", Version=\"0.1.0\""
                    .formatted(deviceId))
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      return Optional.empty();
    }
    JsonNode root = MAPPER.readTree(response.body());
    JsonNode user = root.path("User");
    return Optional.of(
        new JellyfinAuthResult(
            user.path("Id").asText(null),
            user.path("Name").asText(null),
            user.path("Policy").path("IsAdministrator").asBoolean(false),
            root.path("AccessToken").asText(null)));
  }

  private void checkOk(HttpResponse<String> response, String action) throws IOException {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Jellyfin returned %d for %s".formatted(response.statusCode(), action));
    }
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
