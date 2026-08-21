package de.oppahansi.kosmos.metadata.anilist;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.metadata.MetadataProvider;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Anime metadata via AniList's public GraphQL API — no API key needed. AniList's {@code Media}
 * search/lookup is public data with no registration or client-identification requirement, unlike
 * AniDB's hash-based file lookup — see {@code AniDbUdpClient}'s own doc comment for that.
 */
@ApplicationScoped
public class AniListMetadataProvider implements MetadataProvider {

  private static final String GRAPHQL_URL = "https://graphql.anilist.co";
  private static final String HTML_TAG = "<[^>]+>";

  private static final String SEARCH_QUERY =
      """
      query ($search: String) {
        Page(page: 1, perPage: 20) {
          media(search: $search, type: ANIME) {
            id
            title { romaji english }
            startDate { year }
            description(asHtml: false)
            coverImage { large }
            bannerImage
            status
            episodes
          }
        }
      }
      """;

  private static final String BY_ID_QUERY =
      """
      query ($id: Int) {
        Media(id: $id, type: ANIME) {
          id
          title { romaji english }
          startDate { year }
          description(asHtml: false)
          coverImage { large }
          status
          episodes
        }
      }
      """;

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String pluginSlug() {
    return "anilist";
  }

  /**
   * Backs the frontend's Anime search tab, previously always empty (TMDB has no anime media type of
   * its own). 24h cache, same reasoning and TTL as TMDB's own search-as-you-type caching.
   */
  @Override
  @CacheResult(cacheName = "anilist-search")
  public List<MetadataSearchResult> search(String query) {
    AniListSearchResponse response =
        post(SEARCH_QUERY, Map.of("search", query), AniListSearchResponse.class);
    List<AniListMedia> media =
        response.data() != null && response.data().Page() != null
            ? response.data().Page().media()
            : List.of();
    return media.stream().map(this::toSearchResult).toList();
  }

  /**
   * Enough detail for {@code AnimeService} to create an {@code Anime} row — title, overview,
   * poster, status, and total episode count. AniList's own per-episode data is too
   * sparse/unreliable to build a real {@code AnimeEpisode} tree from directly — Fribb/TheXEM close
   * that gap by cross-referencing to a TVDB entry that actually has one.
   */
  @CacheResult(cacheName = "anilist-media")
  public Optional<AniListAnimeDetails> fetchById(String externalId) {
    AniListMediaResponse response =
        post(BY_ID_QUERY, Map.of("id", Integer.valueOf(externalId)), AniListMediaResponse.class);
    AniListMedia media = response.data() != null ? response.data().Media() : null;
    if (media == null) {
      return Optional.empty();
    }
    return Optional.of(
        new AniListAnimeDetails(
            title(media),
            stripHtml(media.description()),
            poster(media),
            media.status(),
            media.episodes()));
  }

  private MetadataSearchResult toSearchResult(AniListMedia media) {
    return new MetadataSearchResult(
        String.valueOf(media.id()),
        title(media),
        media.startDate() != null ? media.startDate().year() : null,
        stripHtml(media.description()),
        poster(media),
        media.bannerImage(),
        null,
        "anime");
  }

  private String title(AniListMedia media) {
    if (media.title() == null) {
      return "Untitled";
    }
    return media.title().english() != null ? media.title().english() : media.title().romaji();
  }

  private String poster(AniListMedia media) {
    return media.coverImage() != null ? media.coverImage().large() : null;
  }

  private String stripHtml(String description) {
    return description == null ? null : description.replaceAll(HTML_TAG, "").trim();
  }

  private <T> T post(String query, Map<String, Object> variables, Class<T> responseType) {
    try {
      String body = objectMapper.writeValueAsString(new GraphQlRequest(query, variables));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(GRAPHQL_URL))
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return objectMapper.readValue(response.body(), responseType);
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("AniList request failed", e);
    }
  }

  private record GraphQlRequest(String query, Map<String, Object> variables) {}
}
