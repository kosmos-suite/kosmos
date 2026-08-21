package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.jellyfin.dto.CreateJellyfinServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
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
}
