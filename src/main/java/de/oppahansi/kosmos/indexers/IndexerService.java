package de.oppahansi.kosmos.indexers;

import de.oppahansi.kosmos.indexers.dto.CreateIndexerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IndexerService {

  public List<Indexer> listAll() {
    return Indexer.listAll();
  }

  public Optional<Indexer> findById(UUID id) {
    return Indexer.findByIdOptional(id);
  }

  @Transactional
  public Indexer create(CreateIndexerRequest request) {
    Indexer indexer = new Indexer();
    indexer.name = request.name();
    indexer.baseUrl = request.baseUrl();
    indexer.apiKey = request.apiKey();
    indexer.enabled = true;
    indexer.createdAt = Instant.now();
    indexer.persist();
    return indexer;
  }
}
