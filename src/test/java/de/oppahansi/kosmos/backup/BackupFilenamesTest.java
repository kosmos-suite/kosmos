package de.oppahansi.kosmos.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.oppahansi.kosmos.backup.BackupFilenames.JdbcTarget;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BackupFilenamesTest {

  @Test
  void newFilenameIsRecognizedAsABackupFilename() {
    String filename = BackupFilenames.newFilename(Instant.parse("2026-08-22T10:15:30Z"));
    assertEquals("kosmos-2026-08-22T10-15-30.dump", filename);
    assertTrue(BackupFilenames.isBackupFilename(filename));
  }

  @Test
  void arbitraryFilenamesAreRejected() {
    assertFalse(BackupFilenames.isBackupFilename("../../etc/passwd"));
    assertFalse(BackupFilenames.isBackupFilename("kosmos-2026-08-22T10-15-30.dump.bak"));
    assertFalse(BackupFilenames.isBackupFilename("not-a-backup.dump"));
  }

  @Test
  void jdbcUrlWithExplicitPortParses() {
    Optional<JdbcTarget> target =
        BackupFilenames.parseJdbcUrl("jdbc:postgresql://localhost:5432/kosmos");
    assertTrue(target.isPresent());
    assertEquals(new JdbcTarget("localhost", "5432", "kosmos"), target.get());
  }

  @Test
  void jdbcUrlWithoutPortDefaultsToFiveFourThreeTwo() {
    Optional<JdbcTarget> target = BackupFilenames.parseJdbcUrl("jdbc:postgresql://db/kosmos");
    assertTrue(target.isPresent());
    assertEquals(new JdbcTarget("db", "5432", "kosmos"), target.get());
  }

  @Test
  void nonPostgresqlUrlDoesNotParse() {
    assertTrue(BackupFilenames.parseJdbcUrl("jdbc:h2:mem:test").isEmpty());
  }

  @Test
  void jdbcUrlWithQueryStringParses() {
    Optional<JdbcTarget> target =
        BackupFilenames.parseJdbcUrl("jdbc:postgresql://localhost:49970/quarkus?loggerLevel=OFF");
    assertTrue(target.isPresent());
    assertEquals(new JdbcTarget("localhost", "49970", "quarkus"), target.get());
  }
}
