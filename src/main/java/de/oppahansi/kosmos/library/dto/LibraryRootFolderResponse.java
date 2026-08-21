package de.oppahansi.kosmos.library.dto;

import de.oppahansi.kosmos.library.LibraryRootFolder;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record LibraryRootFolderResponse(
    UUID id, String path, List<String> contentTypes, Instant createdAt) {

  public static LibraryRootFolderResponse from(LibraryRootFolder folder) {
    List<String> contentTypes =
        folder.contentTypes == null || folder.contentTypes.isBlank()
            ? List.of()
            : Arrays.asList(folder.contentTypes.split(","));
    return new LibraryRootFolderResponse(folder.id, folder.path, contentTypes, folder.createdAt);
  }
}
