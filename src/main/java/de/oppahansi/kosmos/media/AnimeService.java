package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.CreateAnimeRequest;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.anilist.AniListAnimeDetails;
import de.oppahansi.kosmos.metadata.anilist.AniListMetadataProvider;
import de.oppahansi.kosmos.metadata.fribb.FribbEntry;
import de.oppahansi.kosmos.metadata.fribb.FribbMappingProvider;
import de.oppahansi.kosmos.metadata.thexem.TheXemMappingProvider;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import de.oppahansi.kosmos.metadata.tmdb.TmdbShowStructure;
import de.oppahansi.kosmos.parsing.AnimeReleaseParser;
import de.oppahansi.kosmos.parsing.QualityProfileService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AnimeService {

  @Inject QualityProfileService qualityProfileService;
  @Inject AniListMetadataProvider aniListMetadataProvider;
  @Inject FribbMappingProvider fribbMappingProvider;
  @Inject TheXemMappingProvider theXemMappingProvider;
  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject ExternalIdLinkService externalIdLinkService;

  public List<Anime> listAll() {
    return Anime.listAll();
  }

  public Optional<Anime> findById(UUID id) {
    return Anime.findByIdOptional(id);
  }

  public List<AnimeEpisode> episodesFor(UUID animeId) {
    return AnimeEpisode.list(
        "anime.mediaItemId = ?1 order by coalesce(absoluteEpisodeNumber, episodeNumber)", animeId);
  }

  /**
   * Unlike {@link MovieService#create}, the AniList fetch isn't best-effort — an anime with no
   * episode count isn't a useful entry to have added, so a fetch failure fails the whole creation
   * rather than leaving an empty shell (same reasoning {@link ShowService#create} uses for TMDB).
   * The episode tree is enriched via Fribb/anime-lists when possible (see {@link #fribbEnrichment})
   * — real titles/overviews/air dates/stills from TMDB instead of AniList's own too-sparse-to-trust
   * per-episode data, plus TheXEM-corrected absolute numbering when this anime's own AniDB id has
   * one — but that enrichment is best-effort throughout: no Fribb mapping, no TMDB cross-reference,
   * no TheXEM map, or a fetch failure at any step all fall back to the same flat {@code "Episode
   * N"} / {@code absoluteEpisodeNumber == episodeNumber} shape this always produced, never blocking
   * anime creation over it.
   */
  @Transactional
  public Anime create(CreateAnimeRequest request) {
    MediaItem mediaItem = new MediaItem();
    mediaItem.contentType = "anime";
    mediaItem.title = request.title();
    mediaItem.year = request.year();
    mediaItem.addedAt = Instant.now();
    mediaItem.persist();

    Anime anime = new Anime();
    anime.mediaItem = mediaItem;
    anime.overview = request.overview();
    anime.posterPath = request.posterPath();
    anime.backdropPath = request.backdropPath();
    anime.qualityProfile = qualityProfileService.resolveOrThrow(request.qualityProfileId());

    if ("anilist".equals(request.pluginSlug()) && request.externalId() != null) {
      AniListAnimeDetails details =
          aniListMetadataProvider
              .fetchById(request.externalId())
              .orElseThrow(
                  () ->
                      new BadRequestException("AniList entry not found: " + request.externalId()));
      anime.status = details.status();
      anime.episodeCountTotal = details.episodeCount();
      anime.persist();
      persistEpisodes(anime, details.episodeCount(), fribbEnrichment(mediaItem, request));
    } else {
      anime.persist();
    }

    if (request.externalId() != null && request.pluginSlug() != null) {
      externalIdLinkService.link(mediaItem, request.pluginSlug(), request.externalId());
    }

    return anime;
  }

  /**
   * Resolves this anime's AniList id through Fribb/anime-lists to a TMDB TV id + season number (for
   * the real episode tree) and an AniDB id (for {@link TheXemMappingProvider}'s absolute- numbering
   * correction), recording whatever other catalog ids the mapping carries along the way.
   * Empty/absent fields throughout are all handled by {@link #persistEpisodes}'s own fallbacks.
   */
  private FribbEnrichment fribbEnrichment(MediaItem mediaItem, CreateAnimeRequest request) {
    int anilistId;
    try {
      anilistId = Integer.parseInt(request.externalId());
    } catch (NumberFormatException e) {
      return new FribbEnrichment(List.of(), null);
    }
    FribbEntry entry = fribbMappingProvider.loadMapping().get(anilistId);
    if (entry == null) {
      return new FribbEnrichment(List.of(), null);
    }
    linkFribbExternalIds(mediaItem, entry);

    Integer tmdbTvId = entry.themoviedbId() != null ? entry.themoviedbId().tv() : null;
    Integer tmdbSeason = entry.season() != null ? entry.season().tmdb() : null;
    List<TmdbShowStructure.EpisodeData> episodes =
        tmdbTvId == null || tmdbSeason == null
            ? List.of()
            : tmdbMetadataProvider.fetchSeasonEpisodes(String.valueOf(tmdbTvId), tmdbSeason);
    return new FribbEnrichment(episodes, entry.anidbId());
  }

  /** TMDB episode metadata plus the AniDB id TheXEM's scene-numbering lookup is keyed on. */
  private record FribbEnrichment(List<TmdbShowStructure.EpisodeData> episodes, Integer anidbId) {}

  private void linkFribbExternalIds(MediaItem mediaItem, FribbEntry entry) {
    if (entry.tvdbId() != null) {
      externalIdLinkService.link(mediaItem, "tvdb", String.valueOf(entry.tvdbId()));
    }
    if (entry.malId() != null) {
      externalIdLinkService.link(mediaItem, "mal", String.valueOf(entry.malId()));
    }
    if (entry.anidbId() != null) {
      externalIdLinkService.link(mediaItem, "anidb", String.valueOf(entry.anidbId()));
    }
    if (entry.imdbId() != null && !entry.imdbId().isEmpty()) {
      externalIdLinkService.link(mediaItem, "imdb", entry.imdbId().get(0));
    }
  }

  @Transactional
  public Optional<Anime> updateQualityProfile(UUID animeId, UUID qualityProfileId) {
    return findById(animeId)
        .map(
            anime -> {
              anime.qualityProfile = qualityProfileService.resolveOrThrow(qualityProfileId);
              return anime;
            });
  }

  /**
   * {@code enrichment.episodes()} is matched in by episode number and is optional per-episode — a
   * title missing from it (TMDB's season having fewer entries than AniList's count, or no mapping
   * at all) just falls back to the {@code "Episode N"} placeholder for that one episode, same as
   * before Fribb enrichment existed. {@code absoluteEpisodeNumber} defaults to the within-cour
   * episode number {@code i} and is only overridden when {@link TheXemMappingProvider} has a real
   * scene-numbering entry for {@code (enrichment.anidbId(), i)} — the fansub-visible absolute count
   * for a long-running show can genuinely differ from a fresh cour's own 1-based count, which is
   * exactly what {@link AnimeReleaseParser}-driven search/grab matches against.
   */
  private void persistEpisodes(Anime anime, Integer episodeCount, FribbEnrichment enrichment) {
    if (episodeCount == null || episodeCount <= 0) {
      return;
    }
    for (int i = 1; i <= episodeCount; i++) {
      int episodeNumber = i;
      TmdbShowStructure.EpisodeData tmdbEpisode =
          enrichment.episodes().stream()
              .filter(e -> e.episodeNumber() == episodeNumber)
              .findFirst()
              .orElse(null);

      MediaItem episodeMediaItem = new MediaItem();
      episodeMediaItem.contentType = "anime_episode";
      episodeMediaItem.title =
          tmdbEpisode != null && tmdbEpisode.title() != null ? tmdbEpisode.title() : "Episode " + i;
      episodeMediaItem.year = anime.mediaItem.year;
      episodeMediaItem.addedAt = Instant.now();
      episodeMediaItem.persist();

      AnimeEpisode episode = new AnimeEpisode();
      episode.mediaItem = episodeMediaItem;
      episode.anime = anime;
      episode.episodeNumber = i;
      episode.absoluteEpisodeNumber =
          enrichment.anidbId() == null
              ? i
              : theXemMappingProvider
                  .sceneAbsoluteForAnidbEpisode(enrichment.anidbId(), i)
                  .orElse(i);
      episode.episodeType = "EPISODE";
      if (tmdbEpisode != null) {
        episode.overview = tmdbEpisode.overview();
        episode.airDate = tmdbEpisode.airDate();
        episode.runtimeMinutes = tmdbEpisode.runtimeMinutes();
        episode.stillPath = tmdbEpisode.stillPath();
      }
      episode.persist();
    }
  }
}
