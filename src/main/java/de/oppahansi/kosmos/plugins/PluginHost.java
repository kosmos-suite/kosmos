package de.oppahansi.kosmos.plugins;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads a plugin's {@code manifest.json} + {@code .wasm} module and instantiates it via Chicory, a
 * pure-JVM WASM runtime — no native deps.
 *
 * <p>Registers exactly one host import, {@code env.http_fetch(ptr, len) -> i64}, scoped per plugin
 * to {@link PluginManifest.Permissions#allowedHosts}: a guest asking for a URL whose host isn't on
 * its own manifest's allowlist gets back a {@code "blocked:<host>"} string instead of a real
 * response, never an actual outbound request. This is the *only* thing a loaded plugin can reach
 * the outside world through — no filesystem, no sockets, no other imports — so the permission
 * surface stays exactly what the manifest declares. Verified against a real compiled guest ({@code
 * ping}, {@code PluginHostTest}): a call for an allowed host reaches the real network; a call for
 * any other host never leaves the process.
 */
@ApplicationScoped
public class PluginHost {

  private static final int MAX_RESPONSE_BYTES = 1_048_576;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  /** {@code pluginDir} must contain {@code manifest.json} and the {@code .wasm} file it names. */
  public LoadedPlugin load(Path pluginDir) throws IOException {
    PluginManifest manifest =
        objectMapper.readValue(
            Files.readString(pluginDir.resolve("manifest.json")), PluginManifest.class);
    try (InputStream wasm = Files.newInputStream(pluginDir.resolve(manifest.entryPoint()))) {
      return load(manifest, wasm);
    }
  }

  /**
   * Loads a plugin bundled into the app's own classpath (e.g. {@code
   * src/main/resources/plugins/<slug>/}) rather than a plugin directory on disk — the shape a
   * first-party plugin needs, since classpath resources aren't real filesystem paths once packaged
   * into a jar/native image, unlike {@link #load(Path)}'s user-installed-plugin case.
   */
  public LoadedPlugin loadFromClasspath(String resourceDir) throws IOException {
    PluginManifest manifest;
    try (InputStream manifestStream = resourceStream(resourceDir + "/manifest.json")) {
      manifest = objectMapper.readValue(manifestStream, PluginManifest.class);
    }
    try (InputStream wasm = resourceStream(resourceDir + "/" + manifest.entryPoint())) {
      return load(manifest, wasm);
    }
  }

  /**
   * Reads a bundled plugin's {@code manifest.json} without instantiating its WASM module — for
   * listing/UI use ({@link PluginResource}), where paying Chicory's instantiation cost for every
   * plugin just to show its name and permissions would be wasteful.
   */
  public PluginManifest readManifestFromClasspath(String resourceDir) throws IOException {
    try (InputStream manifestStream = resourceStream(resourceDir + "/manifest.json")) {
      return objectMapper.readValue(manifestStream, PluginManifest.class);
    }
  }

  private InputStream resourceStream(String resourcePath) throws IOException {
    InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (stream == null) {
      throw new IOException("Plugin resource not found on classpath: " + resourcePath);
    }
    return stream;
  }

  private LoadedPlugin load(PluginManifest manifest, InputStream wasm) {
    WasmModule module = Parser.parse(wasm);
    ImportValues imports =
        ImportValues.builder()
            .addFunction(httpFetchFunction(manifest))
            .addFunction(abortFunction())
            .build();
    Instance instance = Instance.builder(module).withImportValues(imports).build();
    initializeIfPresent(instance);
    return new LoadedPlugin(manifest, instance);
  }

  /**
   * TinyGo's {@code wasm-unknown} target emits a reactor-style {@code _initialize} export that must
   * run once before other exports are safe to call — Chicory only auto-invokes the WASI command
   * convention ({@code _start}), not this one. Absent for Rust/AssemblyScript guests, so this is a
   * no-op for them.
   */
  private void initializeIfPresent(Instance instance) {
    try {
      instance.export("_initialize").apply();
    } catch (InvalidException e) {
      // No such export — this guest doesn't use the reactor convention.
    }
  }

  /**
   * AssemblyScript-compiled guests unconditionally import {@code env.abort(msgPtr, filePtr, line,
   * col)} for runtime assertion/bounds-check failures — WASM instantiation fails outright if any
   * declared import has no value, even one a guest never actually calls at runtime. Rust guests
   * don't declare this import (panics compile to a bare {@code unreachable} trap instead), so
   * registering it unconditionally is harmless for them.
   */
  private HostFunction abortFunction() {
    return new HostFunction(
        "env",
        "abort",
        FunctionType.of(List.of(ValType.I32, ValType.I32, ValType.I32, ValType.I32), List.of()),
        (Instance instance, long... args) -> {
          throw new IllegalStateException(
              "AssemblyScript guest aborted at line " + args[2] + ", column " + args[3]);
        });
  }

  private HostFunction httpFetchFunction(PluginManifest manifest) {
    return new HostFunction(
        "env",
        "http_fetch",
        FunctionType.of(List.of(ValType.I32, ValType.I32), List.of(ValType.I64)),
        (Instance instance, long... args) -> {
          String url = instance.memory().readString((int) args[0], (int) args[1]);
          String body = fetch(manifest, url);
          return new long[] {writeIntoGuest(instance, body)};
        });
  }

  private String fetch(PluginManifest manifest, String url) {
    String host;
    try {
      host = URI.create(url).getHost();
    } catch (IllegalArgumentException e) {
      return "blocked:invalid-url";
    }
    if (!manifest.permissions().allowsHost(host)) {
      return "blocked:" + host;
    }
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String body = response.body();
      return body.length() > MAX_RESPONSE_BYTES ? body.substring(0, MAX_RESPONSE_BYTES) : body;
    } catch (IOException | InterruptedException e) {
      return "error:fetch-failed";
    }
  }

  /** Allocates space in the *guest's* memory (its own {@code alloc} export) and writes into it. */
  private long writeIntoGuest(Instance instance, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    int ptr = (int) instance.export("alloc").apply(bytes.length)[0];
    instance.memory().write(ptr, bytes);
    return PluginAbi.pack(ptr, bytes.length);
  }
}
