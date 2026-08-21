package de.oppahansi.kosmos.metadata.wasm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.plugins.PluginHost;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class WasmTmdbMetadataProviderTest {

  @Test
  void throwsWhenApiKeyIsNotConfigured() {
    WasmTmdbMetadataProvider provider = new WasmTmdbMetadataProvider();
    provider.apiKey = Optional.empty();

    assertThrows(IllegalStateException.class, () -> provider.search("Inception"));
  }

  /**
   * Not run in an environment without a real TMDB key — skipped, not failed, same reasoning {@code
   * KOSMOS_TEST_JELLYFIN_USERNAME}-gated verification would use if this project had one. When it
   * does run (locally, with {@code .env} sourced into the shell), it's a genuine end-to-end proof:
   * real Chicory instantiation of the compiled {@code tmdb-search} guest (source in the separate
   * kosmos-plugin-tmdb-search repo), which itself makes a real HTTP call to the real TMDB API
   * through {@link PluginHost}'s permission-scoped {@code http_fetch} import — not a stub anywhere
   * in the chain.
   */
  @Test
  @EnabledIfEnvironmentVariable(named = "KOSMOS_METADATA_TMDB_API_KEY", matches = ".+")
  void searchesRealTmdbThroughTheWasmGuest() {
    WasmTmdbMetadataProvider provider = new WasmTmdbMetadataProvider();
    provider.apiKey = Optional.of(System.getenv("KOSMOS_METADATA_TMDB_API_KEY"));
    provider.pluginHost = new PluginHost();

    List<MetadataSearchResult> results = provider.search("Inception");

    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(r -> "Inception".equals(r.title())));
  }
}
