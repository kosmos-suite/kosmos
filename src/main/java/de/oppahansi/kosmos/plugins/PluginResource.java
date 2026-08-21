package de.oppahansi.kosmos.plugins;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Exposes {@link BundledPlugins#ALL}'s manifests, permissions included. Only lists first-party
 * bundled plugins today; a user-installed-plugin listing would extend this.
 */
@Path("/plugins")
@Produces(MediaType.APPLICATION_JSON)
public class PluginResource {

  @Inject PluginHost pluginHost;
  @Inject PluginRegistryClient registryClient;

  @GET
  public List<PluginManifest> listInstalled() {
    return BundledPlugins.ALL.stream().map(this::readManifest).toList();
  }

  /** Read-only discovery listing from kosmos-plugin-registry — no install support yet. */
  @GET
  @Path("/registry")
  public List<RegistryEntry> listRegistry() {
    return registryClient.listEntries();
  }

  private PluginManifest readManifest(String resourceDir) {
    try {
      return pluginHost.readManifestFromClasspath(resourceDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
