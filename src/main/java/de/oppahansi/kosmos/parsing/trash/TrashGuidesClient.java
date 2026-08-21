package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Fetches the real Radarr custom-format definitions from github.com/TRaSH-Guides/Guides. */
@ApplicationScoped
public class TrashGuidesClient {

  private static final String LIST_URL =
      "https://api.github.com/repos/TRaSH-Guides/Guides/contents/docs/json/radarr/cf";

  /**
   * These TRaSH-Guides categories are Radarr-specific naming conventions for non-English/anime
   * releases that don't apply here — {@link de.oppahansi.kosmos.parsing.dto.ParsedRelease} (movie
   * scoring) has no language field, and Kosmos's own anime matching runs through a separate
   * pipeline ({@link de.oppahansi.kosmos.parsing.dto.ParsedAnimeRelease}), not TRaSH custom
   * formats. Filtered out here rather than fetched and then discarded downstream, since
   * TRaSH-Guides names its files by category.
   */
  private static final List<String> EXCLUDED_PREFIXES =
      List.of("anime-", "french-", "german-", "asian-", "dual-audio", "fansub", "language-");

  private static final int MAX_ATTEMPTS = 3;

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Fetches every in-scope file. A handful of the ~170 concurrent requests to
   * raw.githubusercontent.com occasionally hit a transient connection reset — each is retried up to
   * {@link #MAX_ATTEMPTS} times before being reported in {@link TrashFetchResult#failedFilenames()}
   * rather than silently dropped, so a caller always knows the full candidate set was accounted
   * for.
   */
  public TrashFetchResult fetchAll() {
    List<GitHubContentEntry> entries = listDirectory().stream().filter(this::inScope).toList();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      record Task(GitHubContentEntry entry, Future<TrashCustomFormatDefinition> future) {}
      List<Task> tasks =
          entries.stream()
              .map(entry -> new Task(entry, executor.submit(() -> fetchWithRetry(entry))))
              .toList();

      List<TrashCustomFormatDefinition> definitions = new ArrayList<>();
      List<String> failed = new ArrayList<>();
      for (Task task : tasks) {
        TrashCustomFormatDefinition definition = getOrNull(task.future());
        if (definition != null) {
          definitions.add(definition);
        } else {
          failed.add(task.entry().name());
        }
      }
      return new TrashFetchResult(definitions, failed);
    }
  }

  private boolean inScope(GitHubContentEntry entry) {
    return EXCLUDED_PREFIXES.stream().noneMatch(entry.name()::startsWith);
  }

  private List<GitHubContentEntry> listDirectory() {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LIST_URL)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "TRaSH-Guides directory listing failed: HTTP " + response.statusCode());
      }
      return List.of(objectMapper.readValue(response.body(), GitHubContentEntry[].class));
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("Failed to list TRaSH-Guides custom formats", e);
    }
  }

  private TrashCustomFormatDefinition fetchWithRetry(GitHubContentEntry entry) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      TrashCustomFormatDefinition definition = fetchDefinition(entry);
      if (definition != null) {
        return definition;
      }
      if (attempt < MAX_ATTEMPTS) {
        sleep(200L * attempt);
      }
    }
    return null;
  }

  private TrashCustomFormatDefinition fetchDefinition(GitHubContentEntry entry) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(entry.downloadUrl())).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return null;
      }
      return objectMapper.readValue(response.body(), TrashCustomFormatDefinition.class);
    } catch (Exception e) {
      return null;
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private TrashCustomFormatDefinition getOrNull(Future<TrashCustomFormatDefinition> future) {
    try {
      return future.get();
    } catch (Exception e) {
      return null;
    }
  }
}
