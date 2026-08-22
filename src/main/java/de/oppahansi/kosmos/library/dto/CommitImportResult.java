package de.oppahansi.kosmos.library.dto;

import java.util.UUID;

/**
 * One item's outcome from {@code BulkImportResource#commit} — a batch of many files, so one bad
 * path (already imported, unreadable, no root folder) never aborts the rest.
 */
public record CommitImportResult(
    String sourcePath, boolean success, String error, UUID libraryFileId) {

  public static CommitImportResult ok(String sourcePath, UUID libraryFileId) {
    return new CommitImportResult(sourcePath, true, null, libraryFileId);
  }

  public static CommitImportResult failed(String sourcePath, String error) {
    return new CommitImportResult(sourcePath, false, error, null);
  }
}
