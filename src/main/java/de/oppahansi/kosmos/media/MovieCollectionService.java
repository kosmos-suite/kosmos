package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.importlists.ImportListService;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.dto.TmdbCollectionResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import de.oppahansi.kosmos.parsing.QualityProfile;
import de.oppahansi.kosmos.parsing.QualityProfileService;
import de.oppahansi.kosmos.requests.RequestService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Backs {@link MovieCollectionResource} and {@link MovieCollectionSyncJob}. */
@ApplicationScoped
public class MovieCollectionService {

  private static final String TMDB_PLUGIN_SLUG = "tmdb";

  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject RequestService requestService;
  @Inject ExternalIdLinkService externalIdLinkService;
  @Inject QualityProfileService qualityProfileService;
  @Inject ImportListService importListService;

  public List<MovieCollection> listAll() {
    return MovieCollection.list("order by createdAt");
  }

  public Optional<MovieCollection> findById(UUID id) {
    return MovieCollection.findByIdOptional(id);
  }

  public Optional<MovieCollection> findByTmdbId(String tmdbCollectionId) {
    return MovieCollection.<MovieCollection>find("tmdbCollectionId", tmdbCollectionId)
        .firstResultOptional();
  }

  public Optional<TmdbCollectionResult> fetchDetail(String tmdbCollectionId) {
    return tmdbMetadataProvider.fetchCollection(tmdbCollectionId);
  }

  @Transactional
  public MovieCollection monitor(String tmdbCollectionId, UUID qualityProfileId) {
    Optional<MovieCollection> existing = findByTmdbId(tmdbCollectionId);
    if (existing.isPresent()) {
      MovieCollection collection = existing.get();
      collection.monitored = true;
      collection.qualityProfile = resolveQualityProfile(qualityProfileId);
      return collection;
    }
    TmdbCollectionResult detail =
        tmdbMetadataProvider
            .fetchCollection(tmdbCollectionId)
            .orElseThrow(
                () -> new NotFoundException("Unknown TMDB collection: " + tmdbCollectionId));
    MovieCollection collection = new MovieCollection();
    collection.tmdbCollectionId = tmdbCollectionId;
    collection.name = detail.name();
    collection.posterPath = detail.posterPath();
    collection.backdropPath = detail.backdropPath();
    collection.monitored = true;
    collection.qualityProfile = resolveQualityProfile(qualityProfileId);
    collection.createdAt = Instant.now();
    collection.persist();
    return collection;
  }

  @Transactional
  public void unmonitor(UUID id) {
    findById(id).ifPresent(collection -> collection.monitored = false);
  }

  /**
   * Fetches {@code collection}'s current TMDB members and files a Request for each one that isn't
   * already in the library, already pending/approved, or excluded — see {@link
   * ImportListService#isBlocked}, the same check {@code ImportListSyncJob} uses.
   */
  public String sync(UUID collectionId) {
    MovieCollection collection =
        findById(collectionId)
            .orElseThrow(
                () -> new NotFoundException("Unknown movie collection id: " + collectionId));
    TmdbCollectionResult detail =
        tmdbMetadataProvider
            .fetchCollection(collection.tmdbCollectionId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "TMDB no longer has collection " + collection.tmdbCollectionId));

    int filed = 0;
    int skipped = 0;
    for (MetadataSearchResult member : detail.members()) {
      if (importListService.isBlocked(TMDB_PLUGIN_SLUG, member.externalId())) {
        skipped++;
        continue;
      }
      try {
        fileOne(collection, member);
        filed++;
      } catch (RuntimeException e) {
        skipped++;
      }
    }
    QuarkusTransaction.requiringNew().run(() -> markSynced(collectionId));
    return filed + " filed, " + skipped + " already known (in library, pending, or excluded).";
  }

  private void fileOne(MovieCollection collection, MetadataSearchResult member) {
    requestService.createFromList(
        "Collection: " + collection.name,
        false,
        "movie",
        member.externalId(),
        TMDB_PLUGIN_SLUG,
        member.title(),
        member.year(),
        member.overview(),
        member.posterPath(),
        member.backdropPath(),
        collection.qualityProfile != null ? collection.qualityProfile.id : null);
  }

  private void markSynced(UUID collectionId) {
    findById(collectionId).ifPresent(collection -> collection.lastSyncedAt = Instant.now());
  }

  private QualityProfile resolveQualityProfile(UUID id) {
    return id != null ? qualityProfileService.resolveOrThrow(id) : null;
  }
}
