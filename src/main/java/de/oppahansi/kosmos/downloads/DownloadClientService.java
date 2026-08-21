package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.downloads.dto.CreateDownloadClientRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DownloadClientService {

  private static final String DEFAULT_TYPE = "QBITTORRENT";

  public List<DownloadClient> listAll() {
    return DownloadClient.listAll();
  }

  public Optional<DownloadClient> findById(UUID id) {
    return DownloadClient.findByIdOptional(id);
  }

  @Transactional
  public DownloadClient create(CreateDownloadClientRequest request) {
    DownloadClient client = new DownloadClient();
    client.name = request.name();
    client.type =
        request.type() == null || request.type().isBlank() ? DEFAULT_TYPE : request.type();
    client.baseUrl = request.baseUrl();
    client.username = request.username();
    client.password = request.password();
    client.category = request.category();
    client.enabled = true;
    client.createdAt = Instant.now();
    client.persist();
    return client;
  }
}
