package de.oppahansi.kosmos.plugins;

import java.util.List;

/**
 * First-party plugins bundled into the app's own classpath (loaded via {@link
 * PluginHost#loadFromClasspath}), listed explicitly rather than discovered — there's no classpath
 * scanning for {@code plugins/*} resource directories, and one entry is honest about how many exist
 * today. {@link PluginResource} uses {@link #ALL} to list what's installed; individual providers
 * like {@code metadata.wasm.WasmTmdbMetadataProvider} reference their own constant here instead of
 * hardcoding the resource path a second time.
 */
public final class BundledPlugins {

  public static final String TMDB_SEARCH = "plugins/tmdb-search";

  public static final List<String> ALL = List.of(TMDB_SEARCH);

  private BundledPlugins() {}
}
