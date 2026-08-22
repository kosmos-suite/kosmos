package de.oppahansi.kosmos.requests;

import de.oppahansi.kosmos.auth.User;
import de.oppahansi.kosmos.media.AnimeService;
import de.oppahansi.kosmos.media.MediaItem;
import de.oppahansi.kosmos.media.MovieService;
import de.oppahansi.kosmos.media.ShowService;
import de.oppahansi.kosmos.media.dto.CreateAnimeRequest;
import de.oppahansi.kosmos.media.dto.CreateMovieRequest;
import de.oppahansi.kosmos.media.dto.CreateShowRequest;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.parsing.QualityProfile;
import de.oppahansi.kosmos.requests.dto.CreateRequestRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RequestService {

  @Inject MovieService movieService;
  @Inject ShowService showService;
  @Inject AnimeService animeService;
  @Inject ExternalIdLinkService externalIdLinkService;

  public List<Request> listFor(User user) {
    if (user.isAdmin()) {
      return Request.listAll();
    }
    return Request.list("requestedBy", user);
  }

  /**
   * One PENDING request per (user, title) — asking twice while the first is still awaiting review
   * doesn't need a second row, and would just clutter the admin queue.
   */
  @Transactional
  public Request create(User requester, CreateRequestRequest body) {
    if (!"movie".equals(body.mediaType())
        && !"tv".equals(body.mediaType())
        && !"anime".equals(body.mediaType())) {
      throw new BadRequestException("mediaType must be \"movie\", \"tv\", or \"anime\"");
    }
    boolean alreadyPending =
        Request.count(
                "requestedBy = ?1 and externalId = ?2 and pluginSlug = ?3 and status = 'PENDING'",
                requester,
                body.externalId(),
                body.pluginSlug())
            > 0;
    if (alreadyPending) {
      throw new BadRequestException("You already have a pending request for this title");
    }

    Request request = new Request();
    request.requestedBy = requester;
    request.mediaType = body.mediaType();
    request.externalId = body.externalId();
    request.pluginSlug = body.pluginSlug();
    request.title = body.title();
    request.year = body.year();
    request.overview = body.overview();
    request.posterPath = body.posterPath();
    request.backdropPath = body.backdropPath();
    request.qualityProfile =
        body.qualityProfileId() != null ? findQualityProfileOrThrow(body.qualityProfileId()) : null;
    request.status = "PENDING";
    request.requestedAt = Instant.now();
    request.persist();
    return request;
  }

  /**
   * The {@link de.oppahansi.kosmos.importlists.ImportListSyncJob} equivalent of {@link #create} —
   * {@code requestedBy} is left null and {@code sourceListName} set instead (see {@link Request}'s
   * own doc). Auto-approves immediately when {@code trusted}, reusing {@link #approve} with a null
   * {@code admin} (that field is nullable exactly for this — a system action has no human decider).
   */
  @Transactional
  public Request createFromList(
      String listName,
      boolean trusted,
      String mediaType,
      String externalId,
      String pluginSlug,
      String title,
      Integer year,
      String overview,
      String posterPath,
      String backdropPath,
      UUID qualityProfileId) {
    Request request = new Request();
    request.sourceListName = listName;
    request.mediaType = mediaType;
    request.externalId = externalId;
    request.pluginSlug = pluginSlug;
    request.title = title;
    request.year = year;
    request.overview = overview;
    request.posterPath = posterPath;
    request.backdropPath = backdropPath;
    request.qualityProfile =
        qualityProfileId != null ? findQualityProfileOrThrow(qualityProfileId) : null;
    request.status = "PENDING";
    request.requestedAt = Instant.now();
    request.persist();
    return trusted ? approve(request.id, null, qualityProfileId) : request;
  }

  /**
   * Reuses the same create path a direct admin add goes through ({@link MovieService#create}/
   * {@link ShowService#create}) — if the title was added to the library by someone else since the
   * request was filed, links the existing item instead of creating a duplicate.
   */
  @Transactional
  public Request approve(UUID requestId, User admin, UUID qualityProfileIdOverride) {
    Request request = requirePending(requestId);
    UUID qualityProfileId =
        qualityProfileIdOverride != null
            ? qualityProfileIdOverride
            : request.qualityProfile != null ? request.qualityProfile.id : null;

    Optional<MediaItem> existing =
        externalIdLinkService.findLinkedMediaItem(request.pluginSlug, request.externalId);
    MediaItem mediaItem;
    if (existing.isPresent()) {
      mediaItem = existing.get();
      switch (request.mediaType) {
        case "movie" -> movieService.updateQualityProfile(mediaItem.id, qualityProfileId);
        case "anime" -> animeService.updateQualityProfile(mediaItem.id, qualityProfileId);
        default -> showService.updateQualityProfile(mediaItem.id, qualityProfileId);
      }
    } else {
      mediaItem =
          switch (request.mediaType) {
            case "movie" ->
                movieService.create(
                        new CreateMovieRequest(
                            request.externalId,
                            request.pluginSlug,
                            request.title,
                            request.year,
                            request.overview,
                            request.posterPath,
                            request.backdropPath,
                            qualityProfileId,
                            null))
                    .mediaItem;
            case "anime" ->
                animeService.create(
                        new CreateAnimeRequest(
                            request.externalId,
                            request.pluginSlug,
                            request.title,
                            request.year,
                            request.overview,
                            request.posterPath,
                            request.backdropPath,
                            qualityProfileId,
                            null))
                    .mediaItem;
            default ->
                showService.create(
                        new CreateShowRequest(
                            request.externalId,
                            request.pluginSlug,
                            request.title,
                            request.year,
                            request.overview,
                            request.posterPath,
                            request.backdropPath,
                            qualityProfileId,
                            null))
                    .mediaItem;
          };
    }

    request.status = "APPROVED";
    request.mediaItem = mediaItem;
    request.decidedBy = admin;
    request.decidedAt = Instant.now();
    return request;
  }

  @Transactional
  public Request decline(UUID requestId, User admin, String note) {
    Request request = requirePending(requestId);
    request.status = "DECLINED";
    request.note = note;
    request.decidedBy = admin;
    request.decidedAt = Instant.now();
    return request;
  }

  private Request requirePending(UUID requestId) {
    Request request =
        Request.<Request>findByIdOptional(requestId).orElseThrow(NotFoundException::new);
    if (!"PENDING".equals(request.status)) {
      throw new ForbiddenException("Request has already been decided");
    }
    return request;
  }

  private QualityProfile findQualityProfileOrThrow(UUID id) {
    return QualityProfile.<QualityProfile>findByIdOptional(id)
        .orElseThrow(() -> new BadRequestException("Unknown quality profile id: " + id));
  }
}
