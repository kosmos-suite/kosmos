package de.oppahansi.kosmos.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Runs the real two-step fetch (directory listing, then each entry's own JSON) against a real local
 * {@link HttpServer} rather than mocking the HTTP client — genuine socket round trips, same
 * response shapes the real GitHub contents API returns.
 */
class PluginRegistryClientTest {

  private HttpServer server;
  private PluginRegistryClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/plugins",
        exchange -> {
          String body =
              """
              [{"name":"ping.json","type":"file","download_url":"%s/ping.json"},
               {"name":".gitkeep","type":"file","download_url":"%s/.gitkeep"}]
              """
                  .formatted(baseUrl(), baseUrl());
          respond(exchange, body);
        });
    server.createContext(
        "/ping.json",
        exchange ->
            respond(
                exchange,
                """
                {"slug":"ping","name":"Ping","description":"Reference plugin.",
                 "category":"Metadata","publisher":"kosmos-suite",
                 "repository":"kosmos-suite/kosmos-plugin-examples","version":"v1.0.0",
                 "checksum":"sha256:%s"}
                """
                    .formatted("a".repeat(64))));
    server.start();

    client = new PluginRegistryClient();
    client.contentsUrl = baseUrl() + "/plugins";
  }

  private String baseUrl() {
    return "http://localhost:" + server.getAddress().getPort();
  }

  private void respond(com.sun.net.httpserver.HttpExchange exchange, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void listsOnlyJsonFilesAndParsesEachEntry() {
    List<RegistryEntry> entries = client.listEntries();

    assertEquals(1, entries.size());
    RegistryEntry entry = entries.get(0);
    assertEquals("ping", entry.slug());
    assertEquals("Ping", entry.name());
    assertEquals("Metadata", entry.category());
    assertEquals("kosmos-suite/kosmos-plugin-examples", entry.repository());
    assertEquals("v1.0.0", entry.version());
  }

  /**
   * Genuinely hits the real, public kosmos-plugin-registry repo — skipped, not faked, without
   * KOSMOS_TEST_LIVE_REGISTRY set, same env-gated-live-verification shape as
   * WasmTmdbMetadataProviderTest. The registry is expected to be empty right now; this only proves
   * the real fetch/parse pipeline runs clean against the real API, not that any entry exists.
   */
  @Test
  @EnabledIfEnvironmentVariable(named = "KOSMOS_TEST_LIVE_REGISTRY", matches = ".+")
  void fetchesTheRealRegistry() {
    PluginRegistryClient realClient = new PluginRegistryClient();
    realClient.contentsUrl =
        "https://api.github.com/repos/kosmos-suite/kosmos-plugin-registry/contents/plugins";

    List<RegistryEntry> entries = realClient.listEntries();

    // No assertion on contents — the registry may be empty. A non-throwing call is the actual
    // thing being verified: the real API is reachable and returns a parseable shape.
    System.out.println("Live registry returned " + entries.size() + " entries");
  }
}
