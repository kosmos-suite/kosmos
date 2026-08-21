package de.oppahansi.kosmos.plugins;

import com.dylibso.chicory.runtime.Instance;
import java.nio.charset.StandardCharsets;

/**
 * A running WASM guest plus the {@link PluginManifest} it was loaded from. {@link #callString}
 * implements the one ABI {@link PluginHost}-loaded plugins share: pass a UTF-8 string in, get a
 * UTF-8 string back, both carried through the guest's own linear memory via its required {@code
 * alloc}/{@code dealloc} exports — see the {@code ping} example's Rust source
 * (kosmos-plugin-examples repo) for the guest side of the same contract.
 */
public final class LoadedPlugin {

  private final PluginManifest manifest;
  private final Instance instance;

  LoadedPlugin(PluginManifest manifest, Instance instance) {
    this.manifest = manifest;
    this.instance = instance;
  }

  public PluginManifest manifest() {
    return manifest;
  }

  /**
   * Calls an exported guest function of shape {@code (ptr: i32, len: i32) -> i64}, where the return
   * value packs a result pointer/length the same way the argument does — see {@link PluginAbi} for
   * the packing scheme, shared with the host-side {@code env.http_fetch} function {@link
   * PluginHost} registers.
   */
  public String callString(String exportName, String arg) {
    byte[] argBytes = arg.getBytes(StandardCharsets.UTF_8);
    int argPtr = allocate(argBytes.length);
    instance.memory().write(argPtr, argBytes);

    long packed = instance.export(exportName).apply(argPtr, argBytes.length)[0];
    int resultPtr = PluginAbi.unpackPtr(packed);
    int resultLen = PluginAbi.unpackLen(packed);
    String result = instance.memory().readString(resultPtr, resultLen);

    deallocate(argPtr, argBytes.length);
    deallocate(resultPtr, resultLen);
    return result;
  }

  int allocate(int len) {
    return (int) instance.export("alloc").apply(len)[0];
  }

  void deallocate(int ptr, int len) {
    instance.export("dealloc").apply(ptr, len);
  }

  Instance instance() {
    return instance;
  }
}
