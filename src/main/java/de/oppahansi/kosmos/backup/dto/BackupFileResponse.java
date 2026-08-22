package de.oppahansi.kosmos.backup.dto;

import de.oppahansi.kosmos.backup.BackupFile;
import java.time.Instant;

public record BackupFileResponse(String filename, long sizeBytes, Instant createdAt) {

  public static BackupFileResponse from(BackupFile file) {
    return new BackupFileResponse(file.filename(), file.sizeBytes(), file.createdAt());
  }
}
