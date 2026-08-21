package de.oppahansi.kosmos.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Same suite as {@link PluginHostTest} and {@link PluginHostAssemblyScriptTest}, run against the
 * real compiled {@code ping-go} guest (Go/TinyGo, source in the separate kosmos-plugin-examples
 * repo) — a third language proving {@link PluginHost} and {@code kosmos-plugin-sdk}'s ABI
 * convention generalize, not accidentally Rust- or AssemblyScript-shaped.
 */
class PluginHostGoTest {

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

    URL manifestUrl = getClass().getResource("/plugins/ping-go/manifest.json");
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
}
