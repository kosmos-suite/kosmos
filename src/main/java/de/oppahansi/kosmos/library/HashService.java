package de.oppahansi.kosmos.library;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.bouncycastle.crypto.digests.MD4Digest;

/**
 * Computes the two checksums anime tooling standardizes on: CRC32 (embedded directly in most fansub
 * filenames — {@code [Group] Show - 01 [A1B2C3D4].mkv} — cheap to verify a release wasn't corrupted
 * or mislabeled) and ed2k (what AniDB's file-identification API keys lookups on). Movies and
 * Western TV don't use either; this is anime-specific plumbing.
 */
public final class HashService {

  /**
   * eD2k's fixed chunk size, 9500 KiB — every real ed2k implementation (eMule, aMule) uses this
   * exact constant.
   */
  private static final int CHUNK_SIZE = 9_728_000;

  private HashService() {}

  public static String computeCrc32(Path path) throws IOException {
    CRC32 crc32 = new CRC32();
    byte[] buffer = new byte[1 << 16];
    try (InputStream in = Files.newInputStream(path)) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        crc32.update(buffer, 0, read);
      }
    }
    return String.format("%08x", crc32.getValue());
  }

  /**
   * MD4 of each {@value #CHUNK_SIZE}-byte chunk; for a file no larger than one chunk, that single
   * chunk's MD4 *is* the ed2k hash. For a larger file, the chunk hashes are concatenated and MD4'd
   * again. Deliberately does not special-case a file whose size is an exact multiple of the chunk
   * size (historically an ambiguous edge case between older/newer eMule versions, which either did
   * or didn't hash a trailing empty chunk) — this follows the more common modern convention of not
   * adding one.
   */
  public static String computeEd2k(Path path) throws IOException {
    List<byte[]> chunkHashes = new ArrayList<>();
    byte[] buffer = new byte[CHUNK_SIZE];
    try (InputStream in = Files.newInputStream(path)) {
      int read;
      while ((read = readFully(in, buffer)) > 0) {
        chunkHashes.add(md4(buffer, read));
      }
    }
    if (chunkHashes.isEmpty()) {
      chunkHashes.add(md4(new byte[0], 0));
    }
    if (chunkHashes.size() == 1) {
      return toHex(chunkHashes.get(0));
    }

    byte[] concatenated = new byte[chunkHashes.size() * 16];
    for (int i = 0; i < chunkHashes.size(); i++) {
      System.arraycopy(chunkHashes.get(i), 0, concatenated, i * 16, 16);
    }
    return toHex(md4(concatenated, concatenated.length));
  }

  private static int readFully(InputStream in, byte[] buffer) throws IOException {
    int total = 0;
    while (total < buffer.length) {
      int read = in.read(buffer, total, buffer.length - total);
      if (read == -1) {
        break;
      }
      total += read;
    }
    return total;
  }

  private static byte[] md4(byte[] data, int length) {
    MD4Digest digest = new MD4Digest();
    digest.update(data, 0, length);
    byte[] out = new byte[digest.getDigestSize()];
    digest.doFinal(out, 0);
    return out;
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
