package de.oppahansi.kosmos.metadata.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class TmdbMetadataProviderTest {

  private static final String SAMPLE_RESPONSE =
      """
      {
        "page": 1,
        "results": [
          {
            "id": 27205,
            "title": "Inception",
            "release_date": "2010-07-15",
            "overview": "Cobb, a skilled thief who commits corporate espionage.",
            "poster_path": "/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg"
          },
          {
            "id": 155,
            "title": "The Dark Knight",
            "release_date": "2008-07-16",
            "overview": "Batman raises the stakes in his war on crime."
          },
          {
            "id": 999999,
            "title": "Untitled Upcoming Film",
            "release_date": "",
            "overview": ""
          }
        ],
        "total_results": 3
      }
      """;

  private final TmdbMetadataProvider provider = new TmdbMetadataProvider();

  @Test
  void parsesAllResults() {
    List<MetadataSearchResult> results = provider.parse(SAMPLE_RESPONSE);
    assertEquals(3, results.size());
  }

  @Test
  void mapsFieldsOfFirstResult() {
    MetadataSearchResult first = provider.parse(SAMPLE_RESPONSE).get(0);

    assertEquals("27205", first.externalId());
    assertEquals("Inception", first.title());
    assertEquals(2010, first.year());
    assertTrue(first.overview().startsWith("Cobb"));
    assertEquals("/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg", first.posterPath());
  }

  @Test
  void leavesPosterPathNullWhenAbsent() {
    MetadataSearchResult second = provider.parse(SAMPLE_RESPONSE).get(1);
    assertNull(second.posterPath());
  }

  @Test
  void leavesYearNullWhenReleaseDateIsBlank() {
    MetadataSearchResult unreleased = provider.parse(SAMPLE_RESPONSE).get(2);
    assertNull(unreleased.year());
  }

  @Test
  void buildsSearchUrlWhenApiKeyConfigured() throws Exception {
    var field = TmdbMetadataProvider.class.getDeclaredField("apiKey");
    field.setAccessible(true);
    field.set(provider, java.util.Optional.of("testkey"));

    String url = provider.buildSearchUrl("The Matrix");

    assertTrue(url.startsWith("https://api.themoviedb.org/3/search/movie?api_key=testkey&query="));
    assertTrue(url.contains("The+Matrix"));
  }
}
