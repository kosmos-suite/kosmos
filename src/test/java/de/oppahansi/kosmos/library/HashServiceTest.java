package de.oppahansi.kosmos.library;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashServiceTest {

  @TempDir Path tempDir;

  // RFC 1320 §A.5's own published MD4 test vectors — a file no larger than one ed2k chunk hashes
  // to plain MD4, so these double as ed2k vectors for the single-chunk case.
  @Test
  void ed2kOfEmptyFileIsPlainMd4OfEmptyInput() throws IOException {
    assertEquals("31d6cfe0d16ae931b73c59d7e0c089c0", HashService.computeEd2k(writeFile("")));
  }

  @Test
  void ed2kOfShortFileIsPlainMd4() throws IOException {
    assertEquals("a448017aaf21d8525fc10ae87aa6729d", HashService.computeEd2k(writeFile("abc")));
  }

  @Test
  void ed2kOfLongerShortFileIsPlainMd4() throws IOException {
    assertEquals(
        "d9130a8164549fe818874806e1c7014b", HashService.computeEd2k(writeFile("message digest")));
  }

  @Test
  void crc32MatchesKnownValueForEmptyInput() throws IOException {
    assertEquals("00000000", HashService.computeCrc32(writeFile("")));
  }

  @Test
  void crc32MatchesKnownValueForAsciiInput() throws IOException {
    // Standard CRC32("abc"), verified independently against Python's zlib.crc32.
    assertEquals("352441c2", HashService.computeCrc32(writeFile("abc")));
  }

  private Path writeFile(String content) throws IOException {
    Path file = tempDir.resolve("test-" + content.hashCode() + ".bin");
    Files.write(file, content.getBytes(StandardCharsets.US_ASCII));
    return file;
  }
}
