package de.oppahansi.kosmos.metadata.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Deserializes a real (trimmed) TMDB {@code /movie/{id}?append_to_response=release_dates} payload —
 * caught a real bug once: the nested release_dates records need their own
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}, since it doesn't cascade from the enclosing
 * record, and TMDB's actual response carries fields (certification, descriptors, note, ...) none of
 * them declare. Without it, every fetchMovieDetails call silently failed and was swallowed by the
 * caller's broad catch-all — invisible without a test exercising the real JSON shape.
 */
class TmdbMovieDetailsTest {

  private static final String MATRIX_RESPONSE =
      """
      {
        "runtime": 136,
        "release_date": "1999-03-31",
        "release_dates": {
          "results": [
            {
              "iso_3166_1": "US",
              "release_dates": [
                {
                  "certification": "R",
                  "descriptors": [],
                  "iso_639_1": "",
                  "note": "Westwood, California",
                  "release_date": "1999-03-24T00:00:00.000Z",
                  "type": 1
                },
                {
                  "certification": "R",
                  "descriptors": [],
                  "iso_639_1": "",
                  "note": "",
                  "release_date": "1999-03-31T00:00:00.000Z",
                  "type": 3
                },
                {
                  "certification": "",
                  "descriptors": [],
                  "iso_639_1": "",
                  "note": "",
                  "release_date": "1999-06-01T00:00:00.000Z",
                  "type": 4
                }
              ]
            },
            {
              "iso_3166_1": "GB",
              "release_dates": [
                {
                  "certification": "15",
                  "descriptors": [],
                  "iso_639_1": "",
                  "note": "",
                  "release_date": "1999-06-11T00:00:00.000Z",
                  "type": 4
                }
              ]
            }
          ]
        }
      }
      """;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializesRuntimeAndReleaseDate() throws Exception {
    TmdbMovieDetails details = objectMapper.readValue(MATRIX_RESPONSE, TmdbMovieDetails.class);
    assertEquals(136, details.runtime());
    assertEquals(LocalDate.of(1999, 3, 31), details.releaseDateAsLocalDate());
  }

  @Test
  void digitalReleaseDateUsPicksTheUsTypeFourEntry() throws Exception {
    TmdbMovieDetails details = objectMapper.readValue(MATRIX_RESPONSE, TmdbMovieDetails.class);
    assertEquals(LocalDate.of(1999, 6, 1), details.digitalReleaseDateUs());
  }

  @Test
  void noUsDigitalEntryReturnsNull() throws Exception {
    String noUsDigital =
        MATRIX_RESPONSE.replace("\"iso_3166_1\": \"US\"", "\"iso_3166_1\": \"DE\"");
    TmdbMovieDetails details = objectMapper.readValue(noUsDigital, TmdbMovieDetails.class);
    assertNull(details.digitalReleaseDateUs());
  }
}
