package de.oppahansi.kosmos.importlists;

import de.oppahansi.kosmos.importlists.dto.CreateImportListRequest;
import de.oppahansi.kosmos.importlists.dto.UpdateImportListRequest;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbDiscoverClient;
import de.oppahansi.kosmos.parsing.QualityProfile;
import de.oppahansi.kosmos.requests.Request;
import de.oppahansi.kosmos.requests.RequestService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Backs {@link ImportListResource} and {@link ImportListSyncJob}. */
@ApplicationScoped
public class ImportListService {

  private static final String TMDB_PLUGIN_SLUG = "tmdb";

  @Inject TmdbDiscoverClient tmdbDiscoverClient;
  @Inject RequestService requestService;
  @Inject ExternalIdLinkService externalIdLinkService;

  public List<ImportList> listAll() {
    return ImportList.list("order by createdAt");
  }

  public Optional<ImportList> findById(UUID id) {
    return ImportList.findByIdOptional(id);
  }

  @Transactional
  public ImportList create(CreateImportListRequest request) {
    ImportListSourceType sourceType = parseSourceType(request.sourceType());
    ImportList list = new ImportList();
    list.name = request.name();
    list.sourceType = sourceType.name();
    list.enabled = true;
    list.trusted = request.trusted();
    list.qualityProfile = resolveQualityProfile(request.qualityProfileId());
    list.createdAt = Instant.now();
    list.persist();
    return list;
  }

  @Transactional
  public ImportList update(UUID id, UpdateImportListRequest request) {
    ImportList list =
        findById(id).orElseThrow(() -> new NotFoundException("Unknown import list id: " + id));
    list.name = request.name();
    list.enabled = request.enabled();
    list.trusted = request.trusted();
    list.qualityProfile = resolveQualityProfile(request.qualityProfileId());
    return list;
  }

  @Transactional
  public void delete(UUID id) {
    ImportList list =
        findById(id).orElseThrow(() -> new NotFoundException("Unknown import list id: " + id));
    list.delete();
  }

  public List<ImportListExclusion> listExclusions() {
    return ImportListExclusion.list("order by excludedAt desc");
  }

  @Transactional
  public void exclude(String pluginSlug, String externalId, String title) {
    boolean exists =
        ImportListExclusion.count("pluginSlug = ?1 and externalId = ?2", pluginSlug, externalId)
            > 0;
    if (exists) {
      return;
    }
    ImportListExclusion exclusion = new ImportListExclusion();
    exclusion.pluginSlug = pluginSlug;
    exclusion.externalId = externalId;
    exclusion.title = title;
    exclusion.excludedAt = Instant.now();
    exclusion.persist();
  }

  @Transactional
  public void removeExclusion(UUID id) {
    ImportListExclusion.deleteById(id);
  }

  /**
   * Fetches {@code list}'s current candidates and files a {@link Request} for each one that isn't
   * already in the library, already pending/approved, or excluded. Returns a human-readable summary
   * for the {@link de.oppahansi.kosmos.scheduler.JobRun} it backs.
   */
  public String sync(UUID listId) {
    ImportList list =
        findById(listId)
            .orElseThrow(() -> new NotFoundException("Unknown import list id: " + listId));
    List<MetadataSearchResult> candidates = fetch(list.parsedSourceType());

    int filed = 0;
    int skipped = 0;
    for (MetadataSearchResult candidate : candidates) {
      if (alreadyHandled(candidate.externalId())) {
        skipped++;
        continue;
      }
      // requestService.createFromList is @Transactional on that bean's own injected proxy, so
      // this already gets its own transaction — a single bad candidate (e.g. this list's saved
      // quality profile having since been deleted) can't roll back everything already filed.
      try {
        fileOne(list, candidate);
        filed++;
      } catch (RuntimeException e) {
        skipped++;
      }
    }
    // QuarkusTransaction.requiringNew(), not @Transactional: this method is called from within
    // this same bean, and a same-class call bypasses the CDI interceptor that @Transactional
    // relies on — see JobRunner/DownloadStatusPollJob for the same idiom against the same trap.
    QuarkusTransaction.requiringNew().run(() -> markSynced(listId));
    return filed + " filed, " + skipped + " already known (in library, pending, or excluded).";
  }

  private void fileOne(ImportList list, MetadataSearchResult candidate) {
    requestService.createFromList(
        "List: " + list.name,
        list.trusted,
        candidate.mediaType(),
        candidate.externalId(),
        TMDB_PLUGIN_SLUG,
        candidate.title(),
        candidate.year(),
        candidate.overview(),
        candidate.posterPath(),
        candidate.backdropPath(),
        list.qualityProfile != null ? list.qualityProfile.id : null);
  }

  private void markSynced(UUID listId) {
    findById(listId).ifPresent(list -> list.lastSyncedAt = Instant.now());
  }

  private boolean alreadyHandled(String externalId) {
    return isBlocked(TMDB_PLUGIN_SLUG, externalId);
  }

  /**
   * Whether a candidate is already accounted for — in the library, already pending/approved as a
   * {@link Request}, or permanently excluded — and so shouldn't be filed again. Shared with {@code
   * MovieCollectionService}: "should this candidate get a new Request" is the same question for a
   * list-sync candidate and a monitored collection's missing member, so it's the same check.
   */
  public boolean isBlocked(String pluginSlug, String externalId) {
    if (externalIdLinkService.findLinkedMediaItem(pluginSlug, externalId).isPresent()) {
      return true;
    }
    if (Request.count(
            "pluginSlug = ?1 and externalId = ?2 and status in ('PENDING', 'APPROVED')",
            pluginSlug,
            externalId)
        > 0) {
      return true;
    }
    return ImportListExclusion.count("pluginSlug = ?1 and externalId = ?2", pluginSlug, externalId)
        > 0;
  }

  private List<MetadataSearchResult> fetch(ImportListSourceType sourceType) {
    return switch (sourceType) {
      case TMDB_POPULAR_MOVIES -> tmdbDiscoverClient.fetchPopularMovies(1, null);
      case TMDB_UPCOMING_MOVIES -> tmdbDiscoverClient.fetchUpcomingMovies(1, null);
      case TMDB_TRENDING_MOVIES -> tmdbDiscoverClient.fetchTrendingMovies("week", 1, null);
      case TMDB_POPULAR_TV -> tmdbDiscoverClient.fetchPopularTv(1, null);
      case TMDB_UPCOMING_TV -> tmdbDiscoverClient.fetchUpcomingTv(1, null);
      case TMDB_TRENDING_TV -> tmdbDiscoverClient.fetchTrendingTv("week", 1, null);
    };
  }

  private ImportListSourceType parseSourceType(String value) {
    try {
      return ImportListSourceType.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Unknown source type: " + value);
    }
  }

  private QualityProfile resolveQualityProfile(UUID id) {
    if (id == null) {
      return null;
    }
    return QualityProfile.<QualityProfile>findByIdOptional(id)
        .orElseThrow(() -> new BadRequestException("Unknown quality profile id: " + id));
  }
}
