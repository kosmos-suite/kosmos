package de.oppahansi.kosmos.plugins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * What a plugin declares about itself, read from a {@code manifest.json} sitting next to its {@code
 * .wasm} module. {@code slug} doubles as the {@link de.oppahansi.kosmos.metadata.Plugin#slug} a
 * WASM-backed provider registers under, same identity {@link
 * de.oppahansi.kosmos.metadata.ExternalIdLinkService} already keys on for built-in sources like
 * {@code "tmdb"}/{@code "anilist"}.
 *
 * <p>{@code permissions.allowedHosts} is the trust surface a user reviews before installing a
 * third-party plugin, served to the frontend by {@link PluginResource} — {@link PluginHost} is what
 * actually enforces it, at the one host function ({@code env.http_fetch}) a guest has any way to
 * reach the outside world through.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginManifest(
    String slug,
    String name,
    String version,
    /** e.g. {@code "metadata"} — mirrors {@link de.oppahansi.kosmos.metadata.Plugin#kind}. */
    String kind,
    /** The {@code .wasm} file's name, resolved relative to the manifest's own directory. */
    String entryPoint,
    Permissions permissions) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Permissions(List<String> allowedHosts) {

    public boolean allowsHost(String host) {
      return allowedHosts != null && host != null && allowedHosts.contains(host);
    }
  }
}
