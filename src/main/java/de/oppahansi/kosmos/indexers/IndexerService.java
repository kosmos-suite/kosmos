package de.oppahansi.kosmos.indexers;

import de.oppahansi.kosmos.indexers.dto.CreateIndexerRequest;
import de.oppahansi.kosmos.indexers.dto.ImportFromProwlarrResult;
import de.oppahansi.kosmos.indexers.dto.TestIndexerResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IndexerService {

  @Inject TorznabClient torznabClient;

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

  public TestIndexerResult testConnection(String baseUrl, String apiKey) {
    try {
      boolean ok = torznabClient.testConnection(baseUrl, apiKey);
      return new TestIndexerResult(ok, ok ? "Connected" : "No valid Torznab response");
    } catch (IOException | InterruptedException e) {
      String message = e.getMessage();
      return new TestIndexerResult(false, message != null ? message : e.getClass().getSimpleName());
    }
  }

  public TestIndexerResult testProwlarrConnection(String baseUrl, String apiKey) {
    try {
      int count = new ProwlarrClient(baseUrl).listIndexers(apiKey).size();
      return new TestIndexerResult(
          true, "Connected — " + count + " indexer" + (count == 1 ? "" : "s") + " found");
    } catch (IOException | InterruptedException e) {
      String message = e.getMessage();
      return new TestIndexerResult(false, message != null ? message : e.getClass().getSimpleName());
    }
  }

  @Transactional
  public ImportFromProwlarrResult importFromProwlarr(String baseUrl, String apiKey) {
    ProwlarrClient client = new ProwlarrClient(baseUrl);
    List<ProwlarrIndexer> prowlarrIndexers;
    try {
      prowlarrIndexers = client.listIndexers(apiKey);
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach Prowlarr: " + e.getMessage());
    }

    int imported = 0;
    int skippedDisabled = 0;
    for (ProwlarrIndexer prowlarrIndexer : prowlarrIndexers) {
      if (!prowlarrIndexer.enabled()) {
        skippedDisabled++;
        continue;
      }
      Indexer indexer = new Indexer();
      indexer.name = prowlarrIndexer.name();
      indexer.baseUrl = client.torznabUrl(prowlarrIndexer.id());
      indexer.apiKey = apiKey;
      indexer.enabled = true;
      indexer.createdAt = Instant.now();
      indexer.persist();
      imported++;
    }
    return new ImportFromProwlarrResult(imported, skippedDisabled);
  }
}
