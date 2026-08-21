package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.CreateShowRequest;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import de.oppahansi.kosmos.metadata.tmdb.TmdbShowStructure;
import de.oppahansi.kosmos.parsing.QualityProfileService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShowService {

  @Inject QualityProfileService qualityProfileService;
  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject ExternalIdLinkService externalIdLinkService;

  public List<Show> listAll() {
    return Show.listAll();
  }

  public Optional<Show> findById(UUID id) {
    return Show.findByIdOptional(id);
  }

  public List<Season> seasonsFor(UUID showId) {
    return Season.list("show.mediaItemId = ?1 order by seasonNumber", showId);
  }

  public List<Episode> episodesFor(UUID seasonId) {
    return Episode.list("season.id = ?1 order by episodeNumber", seasonId);
  }

  /**
   * Unlike {@link MovieService#create}, the TMDB structure fetch here isn't best-effort — a show
   * with no season/episode tree isn't a useful show to have added, so a fetch failure fails the
   * whole creation rather than leaving an empty shell (see
   * TmdbMetadataProvider#fetchShowStructure).
   */
  @Transactional
  public Show create(CreateShowRequest request) {
    MediaItem mediaItem = new MediaItem();
    mediaItem.contentType = "show";
    mediaItem.title = request.title();
    mediaItem.year = request.year();
    mediaItem.addedAt = Instant.now();
    mediaItem.persist();

    Show show = new Show();
    show.mediaItem = mediaItem;
    show.overview = request.overview();
    show.posterPath = request.posterPath();
    show.backdropPath = request.backdropPath();
    show.qualityProfile = qualityProfileService.resolveOrThrow(request.qualityProfileId());

    if ("tmdb".equals(request.pluginSlug()) && request.externalId() != null) {
      TmdbShowStructure structure = tmdbMetadataProvider.fetchShowStructure(request.externalId());
      show.status = structure.status();
      show.persist();
      persistStructure(show, structure);
    } else {
      show.persist();
    }

    if (request.externalId() != null && request.pluginSlug() != null) {
      externalIdLinkService.link(mediaItem, request.pluginSlug(), request.externalId());
    }

    return show;
  }

  @Transactional
  public Optional<Show> updateQualityProfile(UUID showId, UUID qualityProfileId) {
    return findById(showId)
        .map(
            show -> {
              show.qualityProfile = qualityProfileService.resolveOrThrow(qualityProfileId);
              return show;
            });
  }

  private void persistStructure(Show show, TmdbShowStructure structure) {
    for (TmdbShowStructure.SeasonData seasonData : structure.seasons()) {
      Season season = new Season();
      season.show = show;
      season.seasonNumber = seasonData.seasonNumber();
      season.name = seasonData.name();
      season.overview = seasonData.overview();
      season.posterPath = seasonData.posterPath();
      season.episodeCount = seasonData.episodeCount();
      season.persist();

      for (TmdbShowStructure.EpisodeData episodeData : seasonData.episodes()) {
        MediaItem episodeMediaItem = new MediaItem();
        episodeMediaItem.contentType = "episode";
        episodeMediaItem.title = episodeData.title();
        episodeMediaItem.year = show.mediaItem.year;
        episodeMediaItem.addedAt = Instant.now();
        episodeMediaItem.persist();

        Episode episode = new Episode();
        episode.mediaItem = episodeMediaItem;
        episode.season = season;
        episode.episodeNumber = episodeData.episodeNumber();
        episode.overview = episodeData.overview();
        episode.airDate = episodeData.airDate();
        episode.runtimeMinutes = episodeData.runtimeMinutes();
        episode.stillPath = episodeData.stillPath();
        episode.persist();
      }
    }
  }
}
