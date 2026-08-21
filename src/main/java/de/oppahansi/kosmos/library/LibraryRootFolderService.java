package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.media.MediaItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LibraryRootFolderService {

  @ConfigProperty(name = "kosmos.library.root-path")
  Optional<String> configuredRootPath;

  public List<LibraryRootFolder> listAll() {
    return LibraryRootFolder.list("ORDER BY createdAt");
  }

  public Optional<LibraryRootFolder> findById(UUID id) {
    return LibraryRootFolder.findByIdOptional(id);
  }

  /** Used at title-creation time: an explicit choice if given, else {@link #getDefault(String)}. */
  @Transactional
  public Optional<LibraryRootFolder> resolveOrDefault(UUID rootFolderId, String contentType) {
    return rootFolderId != null ? findById(rootFolderId) : getDefault(contentType);
  }

  public Optional<LibraryRootFolder> getDefault() {
    return getDefault(null);
  }

  /**
   * Prefers a folder tagged for {@code contentType} (or untagged, meaning "accepts anything") over
   * one tagged for something else; falls back to the oldest folder regardless, or a folder seeded
   * from the legacy kosmos.library.root-path env var on first boot if none exist yet at all.
   */
  @Transactional
  public Optional<LibraryRootFolder> getDefault(String contentType) {
    List<LibraryRootFolder> all = listAll();
    if (contentType != null) {
      Optional<LibraryRootFolder> matching =
          all.stream().filter(folder -> accepts(folder, contentType)).findFirst();
      if (matching.isPresent()) {
        return matching;
      }
    }
    if (!all.isEmpty()) {
      return Optional.of(all.get(0));
    }
    return configuredRootPath
        .filter(path -> !path.isBlank())
        .map(path -> createInternal(path, null));
  }

  private boolean accepts(LibraryRootFolder folder, String contentType) {
    if (folder.contentTypes == null || folder.contentTypes.isBlank()) {
      return true;
    }
    return Arrays.asList(folder.contentTypes.split(",")).contains(contentType);
  }

  @Transactional
  public LibraryRootFolder create(String path, List<String> contentTypes) {
    if (path == null || path.isBlank()) {
      throw new BadRequestException("Path is required");
    }
    if (!new File(path).isDirectory()) {
      throw new BadRequestException("Not a directory Kosmos can see: " + path);
    }
    if (LibraryRootFolder.find("path", path).firstResultOptional().isPresent()) {
      throw new BadRequestException("Already registered: " + path);
    }
    return createInternal(path, contentTypes);
  }

  private LibraryRootFolder createInternal(String path, List<String> contentTypes) {
    LibraryRootFolder folder = new LibraryRootFolder();
    folder.path = path;
    folder.contentTypes =
        contentTypes == null || contentTypes.isEmpty() ? null : String.join(",", contentTypes);
    folder.createdAt = Instant.now();
    folder.persist();
    return folder;
  }

  @Transactional
  public void delete(UUID id) {
    LibraryRootFolder folder =
        findById(id).orElseThrow(() -> new NotFoundException("Unknown root folder id: " + id));
    long inUse = MediaItem.count("rootFolder", folder);
    if (inUse > 0) {
      throw new BadRequestException(
          "Still used by " + inUse + " title" + (inUse == 1 ? "" : "s") + " — move them first");
    }
    folder.delete();
  }
}
