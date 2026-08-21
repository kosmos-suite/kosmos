package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.QualityDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Imports the real size-per-minute floors TRaSH-Guides publishes for Radarr
 * (docs/json/radarr/quality-size/movie.json) into {@link QualityDefinition} rows. Radarr's own
 * default table (Quality.DefaultQualityDefinitions) turned out to just be a flat 0–100 MB/min
 * placeholder for everything below Bluray-1080p — TRaSH's numbers are the actually-tuned reference
 * data, same role their custom-format definitions play for scoring (see TrashGuidesImportService).
 */
@ApplicationScoped
public class QualityDefinitionImportService {

  private static final String URL =
      "https://raw.githubusercontent.com/TRaSH-Guides/Guides/master/docs/json/radarr/quality-size/movie.json";

  /**
   * TRaSH keys these by Radarr's combined resolution+source "Quality" enum; Kosmos tracks
   * resolution and source as separate fields, so each TRaSH quality name maps to a (resolution,
   * source) pair here. Remux-1080p/Remux-2160p have no entry — Kosmos's Source enum has no "Remux"
   * concept (it's a modifier, not a source, same reason the TRaSH custom-format importer skips
   * QualityModifierSpecification) — so they're silently skipped, not approximated.
   */
  private static final Map<String, String[]> QUALITY_TO_RESOLUTION_SOURCE =
      Map.ofEntries(
          Map.entry("HDTV-720p", new String[] {"720p", "HDTV"}),
          Map.entry("WEBDL-720p", new String[] {"720p", "Web"}),
          Map.entry("WEBRip-720p", new String[] {"720p", "Web"}),
          Map.entry("Bluray-720p", new String[] {"720p", "Blu-ray"}),
          Map.entry("HDTV-1080p", new String[] {"1080p", "HDTV"}),
          Map.entry("WEBDL-1080p", new String[] {"1080p", "Web"}),
          Map.entry("WEBRip-1080p", new String[] {"1080p", "Web"}),
          Map.entry("Bluray-1080p", new String[] {"1080p", "Blu-ray"}),
          Map.entry("HDTV-2160p", new String[] {"2160p", "HDTV"}),
          Map.entry("WEBDL-2160p", new String[] {"2160p", "Web"}),
          Map.entry("WEBRip-2160p", new String[] {"2160p", "Web"}),
          Map.entry("Bluray-2160p", new String[] {"2160p", "Blu-ray"}));

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Transactional
  public TrashImportResult importAll() {
    TrashQualitySize document = fetch();
    int created = 0;
    int updated = 0;
    List<String> skipped = new ArrayList<>();

    for (TrashQualitySize.Entry entry : document.qualities()) {
      String[] mapped = QUALITY_TO_RESOLUTION_SOURCE.get(entry.quality());
      if (mapped == null) {
        skipped.add(entry.quality() + " (no Kosmos resolution+source equivalent)");
        continue;
      }
      String resolution = mapped[0];
      String source = mapped[1];

      var existing =
          QualityDefinition.<QualityDefinition>find(
                  "resolution = ?1 and source = ?2", resolution, source)
              .firstResultOptional();
      QualityDefinition definition = existing.orElse(null);
      if (definition == null) {
        definition = new QualityDefinition();
        definition.resolution = resolution;
        definition.source = source;
        created++;
      } else {
        updated++;
      }
      definition.minMbPerMinute = entry.min();
      definition.maxMbPerMinute = entry.max();
      definition.persist();
    }

    return new TrashImportResult(created, updated, skipped);
  }

  private TrashQualitySize fetch() {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "TRaSH-Guides quality-size fetch failed: HTTP " + response.statusCode());
      }
      return objectMapper.readValue(response.body(), TrashQualitySize.class);
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("Failed to fetch TRaSH-Guides quality-size reference", e);
    }
  }
}
