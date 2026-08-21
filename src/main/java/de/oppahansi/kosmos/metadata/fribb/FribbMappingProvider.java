package de.oppahansi.kosmos.metadata.fribb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fribb/anime-lists' anime-id cross-reference table — a single static ~7.5MB JSON file on GitHub,
 * no auth or registration needed (unlike AniDB), actively maintained as the successor to the now-
 * archived manami-project/anime-offline-database. Loaded and cached as a single blob keyed by
 * AniList id, the same "whole thing, long TTL" shape already used for {@code tmdb-trending}/{@code
 * tmdb-popular} — this file changes on the order of days/weeks, not per request, and re-fetching
 * 7.5MB on every anime add would be wasteful.
 *
 * <p>Best-effort: this is an enrichment source, not a required one — {@code media.AnimeService}
 * already has a perfectly usable flat-episode fallback from AniList alone, so a fetch failure here
 * returns an empty map rather than blocking anime creation.
 */
@ApplicationScoped
public class FribbMappingProvider {

  private static final String MAPPING_URL =
      "https://raw.githubusercontent.com/Fribb/anime-lists/master/anime-list-full.json";

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @CacheResult(cacheName = "fribb-mapping")
  public Map<Integer, FribbEntry> loadMapping() {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(MAPPING_URL)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Map.of();
      }
      List<FribbEntry> entries =
          objectMapper.readValue(response.body(), new TypeReference<List<FribbEntry>>() {});
      return entries.stream()
          .filter(e -> e.anilistId() != null)
          .collect(Collectors.toMap(FribbEntry::anilistId, e -> e, (a, b) -> a));
    } catch (IOException | InterruptedException e) {
      return Map.of();
    }
  }
}
