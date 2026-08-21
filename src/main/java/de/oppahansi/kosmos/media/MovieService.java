package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.CreateMovieRequest;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import de.oppahansi.kosmos.parsing.QualityProfileService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MovieService {

  @Inject QualityProfileService qualityProfileService;
  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject ExternalIdLinkService externalIdLinkService;

  public List<Movie> listAll() {
    return Movie.listAll();
  }

  public Optional<Movie> findById(UUID id) {
    return Movie.findByIdOptional(id);
  }

  @Transactional
  public Movie create(CreateMovieRequest request) {
    MediaItem mediaItem = new MediaItem();
    mediaItem.contentType = "movie";
    mediaItem.title = request.title();
    mediaItem.year = request.year();
    mediaItem.addedAt = Instant.now();
    mediaItem.persist();

    Movie movie = new Movie();
    movie.mediaItem = mediaItem;
    movie.overview = request.overview();
    movie.posterPath = request.posterPath();
    movie.backdropPath = request.backdropPath();
    movie.qualityProfile = qualityProfileService.resolveOrThrow(request.qualityProfileId());

    if ("tmdb".equals(request.pluginSlug()) && request.externalId() != null) {
      movie.runtimeMinutes =
          tmdbMetadataProvider.fetchRuntimeMinutes(request.externalId()).orElse(null);
    }

    movie.persist();

    if (request.externalId() != null && request.pluginSlug() != null) {
      externalIdLinkService.link(mediaItem, request.pluginSlug(), request.externalId());
    }

    return movie;
  }

  @Transactional
  public Optional<Movie> updateQualityProfile(UUID movieId, UUID qualityProfileId) {
    return findById(movieId)
        .map(
            movie -> {
              movie.qualityProfile = qualityProfileService.resolveOrThrow(qualityProfileId);
              return movie;
            });
  }
}
