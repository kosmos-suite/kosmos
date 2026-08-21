package de.oppahansi.kosmos.plugins;

/**
 * The pointer/length packing every {@link PluginHost}-loaded guest function and host import agrees
 * on: a single {@code i64} return, high 32 bits the length, low 32 bits the pointer. WASM's own
 * multi-value returns would do this natively, but stable Rust's {@code extern "C"} FFI can't
 * express a multi-return import/export without nightly or a bindgen tool — this packing is the
 * portable option every guest SDK needs to follow too.
 */
final class PluginAbi {

  private PluginAbi() {}

  static long pack(int ptr, int len) {
    return ((long) len << 32) | (ptr & 0xFFFF_FFFFL);
  }

  static int unpackPtr(long packed) {
    return (int) packed;
  }

  static int unpackLen(long packed) {
    return (int) (packed >>> 32);
  }
}
