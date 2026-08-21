package de.oppahansi.kosmos.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loads the real compiled {@code ping} guest (source in the separate kosmos-plugin-examples repo;
 * the built {@code .wasm} is checked in under {@code src/test/resources/plugins/ping} as the test
 * fixture, same convention as {@link de.oppahansi.kosmos.indexers.TorznabClientTest}'s sample
 * payload) and runs it through actual Chicory instantiation — not a stand-in for the WASM host.
 *
 * <p>The permission test spins up a real local {@link HttpServer} rather than reaching the public
 * internet, so the suite stays hermetic; it's still a genuine socket round-trip through the guest's
 * {@code env.http_fetch} import, not a mock of one.
 */
class PluginHostTest {

  private HttpServer server;
  private LoadedPlugin plugin;

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/body",
        exchange -> {
          byte[] body = "hello from host".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();

    URL manifestUrl = getClass().getResource("/plugins/ping/manifest.json");
    Path pluginDir = Path.of(manifestUrl.toURI()).getParent();
    plugin = new PluginHost().load(pluginDir);
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void callsExportedFunctionThroughGuestMemory() {
    assertEquals("Hello, World!", plugin.callString("greet", "World"));
  }

  @Test
  void allowsAFetchToAHostOnTheManifestAllowlist() {
    String url = "http://localhost:" + server.getAddress().getPort() + "/body";
    assertEquals("hello from host", plugin.callString("fetch_allowed", url));
  }

  @Test
  void blocksAFetchToAHostNotOnTheManifestAllowlist() {
    assertEquals(
        "blocked:not-on-the-allowlist.example",
        plugin.callString("fetch_allowed", "http://not-on-the-allowlist.example/body"));
  }

  @Test
  void readsAManifestFromTheClasspathWithoutInstantiatingTheModule() throws IOException {
    PluginManifest manifest = new PluginHost().readManifestFromClasspath("plugins/tmdb-search");

    assertEquals("tmdb-wasm", manifest.slug());
    assertEquals(List.of("api.themoviedb.org"), manifest.permissions().allowedHosts());
  }
}
