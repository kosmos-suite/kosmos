package de.oppahansi.kosmos.backup;

import de.oppahansi.kosmos.backup.BackupFilenames.JdbcTarget;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A full logical dump of the Kosmos database via {@code pg_dump}, in Postgres's own custom archive
 * format (restorable with {@code pg_restore} — not implemented here yet; see the backup roadmap
 * item for why restore is a deliberately separate, more carefully reviewed piece of work than
 * "write files to disk"). Shells out rather than reimplementing a dumper over JDBC: pg_dump is the
 * correct, battle-tested tool for this, and every self-hosted Postgres deployment already needs the
 * client tools installed to administer the database at all.
 */
@ApplicationScoped
public class BackupService {

  @ConfigProperty(name = "kosmos.backup.dir")
  String backupDir;

  @ConfigProperty(name = "kosmos.backup.retention")
  int retention;

  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  String jdbcUrl;

  @ConfigProperty(name = "quarkus.datasource.username")
  String username;

  @ConfigProperty(name = "quarkus.datasource.password")
  String password;

  /** Runs {@code pg_dump}, applies retention, and returns a message describing what was written. */
  public String createBackup() {
    JdbcTarget target =
        BackupFilenames.parseJdbcUrl(jdbcUrl)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Can't parse datasource JDBC URL for pg_dump: " + jdbcUrl));

    Path dir = Path.of(backupDir);
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    String filename = BackupFilenames.newFilename(Instant.now());
    Path dumpFile = dir.resolve(filename);

    ProcessBuilder builder =
        new ProcessBuilder(
            "pg_dump",
            "-h",
            target.host(),
            "-p",
            target.port(),
            "-U",
            username,
            "-F",
            "c",
            "-f",
            dumpFile.toString(),
            target.database());
    builder.environment().put("PGPASSWORD", password);
    builder.redirectErrorStream(true);

    Process process;
    try {
      process = builder.start();
    } catch (IOException e) {
      throw new IllegalStateException(
          "pg_dump not found — install the postgresql-client package on this host.", e);
    }

    String output;
    try {
      output = new String(process.getInputStream().readAllBytes());
      boolean finished = process.waitFor(10, TimeUnit.MINUTES);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalStateException("pg_dump timed out after 10 minutes.");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for pg_dump.", e);
    }

    if (process.exitValue() != 0) {
      try {
        Files.deleteIfExists(dumpFile);
      } catch (IOException ignored) {
        // Best-effort cleanup of a partial/failed dump file.
      }
      throw new IllegalStateException("pg_dump failed: " + output.trim());
    }

    applyRetention(dir);

    long sizeBytes;
    try {
      sizeBytes = Files.size(dumpFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return "Wrote " + filename + " (" + (sizeBytes / 1024 / 1024) + " MB)";
  }

  private void applyRetention(Path dir) {
    List<Path> backups = listBackupPaths(dir);
    if (backups.size() <= retention) {
      return;
    }
    backups.stream()
        .sorted(Comparator.comparing(this::lastModified))
        .limit(backups.size() - retention)
        .forEach(
            path -> {
              try {
                Files.delete(path);
              } catch (IOException ignored) {
                // Next run's retention pass will retry.
              }
            });
  }

  public List<BackupFile> listBackups() {
    Path dir = Path.of(backupDir);
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    return listBackupPaths(dir).stream()
        .map(
            path -> {
              try {
                return new BackupFile(
                    path.getFileName().toString(),
                    Files.size(path),
                    Files.getLastModifiedTime(path).toInstant());
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            })
        .sorted(Comparator.comparing(BackupFile::createdAt).reversed())
        .toList();
  }

  public void deleteBackup(String filename) {
    if (!BackupFilenames.isBackupFilename(filename)) {
      throw new BadRequestException("Not a backup filename: " + filename);
    }
    Path target = Path.of(backupDir).resolve(filename);
    if (!Files.exists(target)) {
      throw new NotFoundException("No such backup: " + filename);
    }
    try {
      Files.delete(target);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private List<Path> listBackupPaths(Path dir) {
    try (var stream = Files.list(dir)) {
      return stream
          .filter(path -> BackupFilenames.isBackupFilename(path.getFileName().toString()))
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Instant lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toInstant();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
