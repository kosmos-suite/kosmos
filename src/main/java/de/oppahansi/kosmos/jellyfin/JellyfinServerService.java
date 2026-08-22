package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.jellyfin.dto.CreateJellyfinServerRequest;
import de.oppahansi.kosmos.jellyfin.dto.RootFolderAutoRegisterResult;
import de.oppahansi.kosmos.jellyfin.dto.TestJellyfinConnectionResult;
import de.oppahansi.kosmos.library.LibraryRootFolderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JellyfinServerService {

  @Inject LibraryRootFolderService rootFolderService;

  public List<JellyfinServer> listAll() {
    return JellyfinServer.listAll();
  }

  public Optional<JellyfinServer> findById(UUID id) {
    return JellyfinServer.findByIdOptional(id);
  }

  /**
   * Same reachability/auth check {@link #listLibraries} does for an already-saved server, but
   * against a baseUrl/apiKey pair that hasn't been persisted yet — what the "Add server" modal's
   * Test Connection button calls before Save is enabled.
   */
  public TestJellyfinConnectionResult testConnection(String baseUrl, String apiKey) {
    try {
      List<JellyfinLibrary> libraries = new JellyfinClient(baseUrl).listLibraries(apiKey);
      int count = libraries.size();
      String message =
          "Connected — found %d %s.".formatted(count, count == 1 ? "library" : "libraries");
      return new TestJellyfinConnectionResult(true, message, count);
    } catch (IOException | InterruptedException e) {
      return new TestJellyfinConnectionResult(
          false, "Could not reach Jellyfin server: " + e.getMessage(), 0);
    }
  }

  @Transactional
  public JellyfinServer create(CreateJellyfinServerRequest request) {
    JellyfinServer server = new JellyfinServer();
    server.name = request.name();
    server.baseUrl = request.baseUrl();
    server.apiKey = request.apiKey();
    server.enabled = true;
    server.createdAt = Instant.now();
    server.persist();
    return server;
  }

  public List<JellyfinLibrary> listLibraries(UUID id) {
    JellyfinServer server = requireServer(id);
    try {
      return new JellyfinClient(server.baseUrl).listLibraries(server.apiKey);
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach Jellyfin server: " + e.getMessage());
    }
  }

  @Transactional
  public void updateSelectedLibraries(UUID id, List<String> libraryIds) {
    JellyfinServer server = requireServer(id);
    server.selectedLibraryIds =
        (libraryIds == null || libraryIds.isEmpty()) ? null : String.join(",", libraryIds);
  }

  /** Empty list means "every library" — the caller's default when nothing has been selected. */
  static List<String> selectedLibraryIds(JellyfinServer server) {
    return server.selectedLibraryIds == null || server.selectedLibraryIds.isBlank()
        ? List.of()
        : Arrays.asList(server.selectedLibraryIds.split(","));
  }

  public List<JellyfinUser> listUsers(UUID id) {
    JellyfinServer server = requireServer(id);
    try {
      return new JellyfinClient(server.baseUrl).listUsers(server.apiKey);
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach Jellyfin server: " + e.getMessage());
    }
  }

  @Transactional
  public void updateSelectedUsers(UUID id, List<String> userIds) {
    JellyfinServer server = requireServer(id);
    server.selectedUserIds =
        (userIds == null || userIds.isEmpty()) ? null : String.join(",", userIds);
  }

  /** Empty list means "every account" — the caller's default when nothing has been selected. */
  static List<String> selectedUserIds(JellyfinServer server) {
    return server.selectedUserIds == null || server.selectedUserIds.isBlank()
        ? List.of()
        : Arrays.asList(server.selectedUserIds.split(","));
  }

  /**
   * Registers a root folder per selected library's real reported path, tagged with the matching
   * Kosmos content type — movies/tvshows only, since a "boxsets" (collections) library is a
   * database-side grouping with no folder of its own. Uses {@link
   * LibraryRootFolderService#createTrusted} rather than the normal validating create: Jellyfin has
   * already vouched for these paths, and Kosmos may not share its filesystem view (a different
   * host, different container mounts) at the moment this runs.
   */
  public RootFolderAutoRegisterResult autoRegisterRootFolders(UUID id) {
    JellyfinServer server = requireServer(id);
    List<JellyfinLibrary> libraries;
    try {
      libraries = new JellyfinClient(server.baseUrl).listLibraries(server.apiKey);
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach Jellyfin server: " + e.getMessage());
    }
    List<String> selected = selectedLibraryIds(server);

    int registered = 0;
    int skipped = 0;
    for (JellyfinLibrary library : libraries) {
      if (!selected.isEmpty() && !selected.contains(library.id())) {
        continue;
      }
      List<String> contentTypes = contentTypesFor(library.collectionType());
      if (contentTypes.isEmpty() || library.locations().isEmpty()) {
        skipped++;
        continue;
      }
      for (String location : library.locations()) {
        if (rootFolderService.createTrusted(location, contentTypes).isPresent()) {
          registered++;
        } else {
          skipped++;
        }
      }
    }
    return new RootFolderAutoRegisterResult(registered, skipped);
  }

  /**
   * "tvshows" accepts both {@code show} and {@code anime}, not just {@code show} — Jellyfin's
   * {@code CollectionType} doesn't distinguish them (both are just "tvshows"; see {@code
   * JellyfinSyncService}'s Fribb-based per-title classification), and this user's own library setup
   * shows why that matters: a library literally named "Anime" and one named "Shows" both report
   * collectionType "tvshows", so tagging the folder narrowly as {@code show} would leave anime
   * titles synced from it unable to find their own root folder.
   */
  private List<String> contentTypesFor(String jellyfinCollectionType) {
    if ("movies".equals(jellyfinCollectionType)) {
      return List.of("movie");
    }
    if ("tvshows".equals(jellyfinCollectionType)) {
      return List.of("show", "anime");
    }
    return List.of();
  }

  private JellyfinServer requireServer(UUID id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Unknown Jellyfin server id: " + id));
  }
}
