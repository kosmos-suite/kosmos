package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.jellyfin.dto.CreateJellyfinServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
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

  public List<JellyfinServer> listAll() {
    return JellyfinServer.listAll();
  }

  public Optional<JellyfinServer> findById(UUID id) {
    return JellyfinServer.findByIdOptional(id);
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

  private JellyfinServer requireServer(UUID id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Unknown Jellyfin server id: " + id));
  }
}
