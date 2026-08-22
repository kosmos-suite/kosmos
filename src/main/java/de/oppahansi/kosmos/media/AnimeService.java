package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.library.LibraryRootFolder;
import de.oppahansi.kosmos.library.LibraryRootFolderService;
import de.oppahansi.kosmos.media.dto.CreateAnimeRequest;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.SimilarEnrichmentService;
import de.oppahansi.kosmos.metadata.anilist.AniListAnimeDetails;
import de.oppahansi.kosmos.metadata.anilist.AniListMetadataProvider;
import de.oppahansi.kosmos.metadata.dto.MediaDetailExtras;
import de.oppahansi.kosmos.metadata.dto.MediaPreview;
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
  @Inject LibraryRootFolderService rootFolderService;
  @Inject AniListMetadataProvider aniListMetadataProvider;
  @Inject FribbMappingProvider fribbMappingProvider;
  @Inject TheXemMappingProvider theXemMappingProvider;
  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject ExternalIdLinkService externalIdLinkService;
  @Inject SimilarEnrichmentService similarEnrichmentService;

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
    mediaItem.rootFolder =
        rootFolderService.resolveOrDefault(request.rootFolderId(), "anime").orElse(null);
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
   * Used by {@code JellyfinSyncService} — same AniList-driven episode tree as {@link #create}, but
   * from a server-reported title/year/root-folder and an already-resolved {@link FribbEntry} (found
   * by reverse Fribb lookup on the Jellyfin item's TMDB id) rather than a user-submitted request,
   * and with no quality profile assigned — matches Jellyfin-synced movies/shows, which are also
   * unmonitored until the user assigns one. Unlike {@link #create}, the AniList fetch failing still
   * fails this call (same reasoning as there): the caller's per-item transaction just gets retried
   * next sync, consistent with how a movie/show's own fetch failure is handled.
   */
  @Transactional
  public Anime createFromJellyfin(
      String title, Integer year, FribbEntry fribbEntry, LibraryRootFolder rootFolder) {
    String anilistId = String.valueOf(fribbEntry.anilistId());
    AniListAnimeDetails details =
        aniListMetadataProvider
            .fetchById(anilistId)
            .orElseThrow(() -> new BadRequestException("AniList entry not found: " + anilistId));

    MediaItem mediaItem = new MediaItem();
    mediaItem.contentType = "anime";
    mediaItem.title = title;
    mediaItem.year = year;
    mediaItem.addedAt = Instant.now();
    mediaItem.rootFolder = rootFolder;
    mediaItem.persist();

    Anime anime = new Anime();
    anime.mediaItem = mediaItem;
    anime.overview = details.overview();
    anime.posterPath = details.posterPath();
    anime.status = details.status();
    anime.episodeCountTotal = details.episodeCount();
    anime.persist();

    linkFribbExternalIds(mediaItem, fribbEntry);
    persistEpisodes(anime, details.episodeCount(), fribbEnrichmentFrom(fribbEntry));

    externalIdLinkService.link(mediaItem, "anilist", anilistId);
    return anime;
  }

  /**
   * Resolves this anime's AniList id through Fribb/anime-lists to a TMDB TV id + season number (for
   * the real episode tree) and an AniDB id (for {@link TheXemMappingProvider}'s absolute- numbering
   * correction), recording whatever other catalog ids the mapping carries along the way.
   * Empty/absent fields throughout are all handled by {@link #persistEpisodes}'s own fallbacks.
   */
  private FribbEnrichment fribbEnrichment(MediaItem mediaItem, CreateAnimeRequest request) {
    FribbEntry entry = resolveFribbEntry(request.externalId()).orElse(null);
    if (entry != null) {
      linkFribbExternalIds(mediaItem, entry);
    }
    return fribbEnrichmentFrom(entry);
  }

  private Optional<FribbEntry> resolveFribbEntry(String anilistExternalId) {
    try {
      int anilistId = Integer.parseInt(anilistExternalId);
      return Optional.ofNullable(fribbMappingProvider.loadMapping().get(anilistId));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private FribbEnrichment fribbEnrichmentFrom(FribbEntry entry) {
    if (entry == null) {
      return new FribbEnrichment(List.of(), null);
    }
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

  /** Genres/similar for the detail page — see {@link AniListMetadataProvider#fetchDetailExtras}. */
  public Optional<MediaDetailExtras> detailExtras(UUID animeId) {
    return externalIdLinkService
        .findActiveExternalId(animeId, "anilist")
        .flatMap(aniListMetadataProvider::fetchDetailExtras)
        .map(
            extras ->
                extras.withSimilar(
                    similarEnrichmentService.enrich(extras.similar(), "anilist", "anime")));
  }

  /**
   * Same idea as {@link #detailExtras}, but for an AniList entry Kosmos has no {@link Anime} row
   * for yet — backs the detail screen a not-in-library card links to, so it opens something real
   * instead of falling back to a search. AniList's by-id lookup carries no backdrop or release year
   * of its own (see {@link AniListAnimeDetails}), so those are left null here rather than guessed
   * at — the "First Aired" fact from {@code extras} still surfaces the year in text form.
   */
  public Optional<MediaPreview> preview(String externalId) {
    Optional<AniListAnimeDetails> base = aniListMetadataProvider.fetchById(externalId);
    if (base.isEmpty()) {
      return Optional.empty();
    }
    AniListAnimeDetails b = base.get();
    MediaDetailExtras extras =
        aniListMetadataProvider
            .fetchDetailExtras(externalId)
            .map(
                e ->
                    e.withSimilar(similarEnrichmentService.enrich(e.similar(), "anilist", "anime")))
            .orElse(
                new MediaDetailExtras(
                    List.of(), List.of(), null, null, null, List.of(), List.of(), null));
    return Optional.of(
        new MediaPreview(
            externalId,
            "anilist",
            "anime",
            b.title(),
            null,
            b.overview(),
            b.posterPath(),
            null,
            extras.genres(),
            extras.facts(),
            extras.voteAverage(),
            extras.voteCount(),
            extras.certification(),
            extras.cast(),
            extras.similar(),
            List.of(),
            previewEpisodes(externalId, b.episodeCount()),
            extras.trailerUrl()));
  }

  /**
   * Same Fribb/TMDB episode enrichment {@link #create} persists, reused read-only for the not-owned
   * preview screen so it can render the identical Episodes section an owned anime's detail page
   * does. Best-effort throughout, same as {@link #fribbEnrichmentFrom} — no mapping or a fetch
   * failure just means the plain {@code "Episode N"} fallback, never a broken preview.
   */
  private List<MediaPreview.PreviewEpisode> previewEpisodes(
      String anilistExternalId, Integer episodeCount) {
    if (episodeCount == null || episodeCount <= 0) {
      return List.of();
    }
    FribbEnrichment enrichment;
    try {
      enrichment = fribbEnrichmentFrom(resolveFribbEntry(anilistExternalId).orElse(null));
    } catch (Exception e) {
      enrichment = new FribbEnrichment(List.of(), null);
    }
    List<MediaPreview.PreviewEpisode> out = new java.util.ArrayList<>();
    for (int i = 1; i <= episodeCount; i++) {
      int episodeNumber = i;
      TmdbShowStructure.EpisodeData tmdbEpisode =
          enrichment.episodes().stream()
              .filter(e -> e.episodeNumber() == episodeNumber)
              .findFirst()
              .orElse(null);
      String title =
          tmdbEpisode != null && tmdbEpisode.title() != null ? tmdbEpisode.title() : "Episode " + i;
      out.add(
          new MediaPreview.PreviewEpisode(
              i, title, tmdbEpisode != null ? tmdbEpisode.airDate() : null));
    }
    return out;
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
      episodeMediaItem.rootFolder = anime.mediaItem.rootFolder;
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
