package de.oppahansi.kosmos.filesystem;

import de.oppahansi.kosmos.filesystem.dto.BrowseResponse;
import de.oppahansi.kosmos.filesystem.dto.DirectoryEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Server-side directory browsing for picking library/root-folder paths — same idea as
 * Radarr/Sonarr's filesystem browser: the whole filesystem the app process can see, not a sandboxed
 * subset, since this always requires the same admin access as the paths it manages.
 */
@ApplicationScoped
public class FilesystemService {

  public BrowseResponse browse(String rawPath) {
    Path dir = rawPath == null || rawPath.isBlank() ? Path.of("/") : Path.of(rawPath);
    if (!Files.isDirectory(dir)) {
      throw new BadRequestException("Not a directory: " + dir);
    }

    List<DirectoryEntry> entries;
    try (Stream<Path> children = Files.list(dir)) {
      entries =
          children
              .filter(Files::isDirectory)
              .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
              .map(p -> new DirectoryEntry(p.getFileName().toString(), p.toString()))
              .toList();
    } catch (IOException e) {
      throw new BadRequestException("Could not read directory: " + e.getMessage());
    }

    String parentPath = dir.getParent() != null ? dir.getParent().toString() : null;
    return new BrowseResponse(dir.toString(), parentPath, entries);
  }
}
