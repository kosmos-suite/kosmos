package de.oppahansi.kosmos.metadata.wasm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.metadata.MetadataProvider;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.plugins.BundledPlugins;
import de.oppahansi.kosmos.plugins.LoadedPlugin;
import de.oppahansi.kosmos.plugins.PluginHost;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * TMDB movie search, run through the real WASM plugin host ({@code tmdb-search}, Rust — source in
 * the separate kosmos-plugin-tmdb-search repo) as a {@link MetadataProvider} implementation.
 * Registered under the {@code "tmdb-wasm"} slug, distinct from {@code "tmdb"} — it runs side by
 * side with the native {@link de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider}, which still
 * backs every production movie/show creation path; {@code TmdbMetadataProvider} implements
 * show/anime-specific methods this class does not.
 *
 * <p>The guest never sees a raw HTTP client: its one way out is {@code env.http_fetch}, scoped by
 * {@code plugins/tmdb-search/manifest.json} to {@code api.themoviedb.org} only. The API key is
 * passed in per call (JVM holds the real secret, same {@code kosmos.metadata.tmdb.api-key} config
 * property {@code TmdbMetadataProvider} reads) rather than baked into the plugin or its manifest.
 */
@ApplicationScoped
public class WasmTmdbMetadataProvider implements MetadataProvider {

  @ConfigProperty(name = "kosmos.metadata.tmdb.api-key")
  Optional<String> apiKey;

  @Inject PluginHost pluginHost;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private volatile LoadedPlugin plugin;

  @Override
  public String pluginSlug() {
    return "tmdb-wasm";
  }

  private record SearchArgs(String query, String apiKey) {}

  @Override
  public List<MetadataSearchResult> search(String query) {
    if (apiKey.isEmpty()) {
      throw new IllegalStateException("kosmos.metadata.tmdb.api-key is not configured");
    }
    try {
      String argsJson = objectMapper.writeValueAsString(new SearchArgs(query, apiKey.get()));
      String resultJson = loadedPlugin().callString("search", argsJson);
      return objectMapper.readValue(resultJson, new TypeReference<List<MetadataSearchResult>>() {});
    } catch (IOException e) {
      throw new UncheckedIOException("tmdb-wasm plugin call failed", e);
    }
  }

  private LoadedPlugin loadedPlugin() {
    LoadedPlugin loaded = plugin;
    if (loaded == null) {
      synchronized (this) {
        loaded = plugin;
        if (loaded == null) {
          try {
            loaded = pluginHost.loadFromClasspath(BundledPlugins.TMDB_SEARCH);
          } catch (IOException e) {
            throw new UncheckedIOException("Failed to load tmdb-wasm plugin", e);
          }
          plugin = loaded;
        }
      }
    }
    return loaded;
  }
}
