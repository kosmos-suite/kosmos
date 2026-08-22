package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.auth.User;
import de.oppahansi.kosmos.jellyfin.dto.JellyfinSyncResult;
import de.oppahansi.kosmos.library.LibraryFile;
import de.oppahansi.kosmos.library.LibraryRootFolder;
import de.oppahansi.kosmos.library.LibraryRootFolderService;
import de.oppahansi.kosmos.library.ProbeService;
import de.oppahansi.kosmos.media.Episode;
import de.oppahansi.kosmos.media.MediaItem;
import de.oppahansi.kosmos.media.Movie;
import de.oppahansi.kosmos.media.Show;
import de.oppahansi.kosmos.media.ShowService;
import de.oppahansi.kosmos.metadata.MediaItemExternalId;
import de.oppahansi.kosmos.metadata.Plugin;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reconciles an already-scanned Jellyfin library against Kosmos's own catalog: the
 * Jellyseerr/Overseerr pattern of reading a media server's own ProviderIds rather than
 * re-identifying anything from scratch. Movies whose TMDB id already exists in Kosmos get a
 * LibraryFile recorded against the existing MediaItem ("already available"); movies Kosmos has
 * never seen get a new MediaItem/Movie bulk-created. Only covers movies with both a Tmdb provider
 * id and a Path — everything else is skipped rather than guessed at. Also syncs Jellyfin's own user
 * list, mirroring each account's admin flag.
 *
 * <p>Every movie and every user gets its own transaction (like {@code DownloadStatusPollJob}): this
 * runs over a potentially large real library, and one bad item (a duplicate path, a colliding
 * username) must never roll back everything else already synced in the same run.
 */
@ApplicationScoped
public class JellyfinSyncService {

  private static final String TMDB_PLUGIN_SLUG = "tmdb";
  private static final String MATCH_METHOD = "JELLYFIN_SYNC";

  @Inject ProbeService probeService;
  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject LibraryRootFolderService rootFolderService;
  @Inject ShowService showService;

  public JellyfinSyncResult sync(UUID serverId) {
    JellyfinServer server =
        QuarkusTransaction.requiringNew()
            .call(
                () ->
                    JellyfinServer.<JellyfinServer>findByIdOptional(serverId)
                        .orElseThrow(
                            () ->
                                new BadRequestException(
                                    "Unknown Jellyfin server id: " + serverId)));

    JellyfinClient client = new JellyfinClient(server.baseUrl);
    List<JellyfinMovie> movies;
    try {
      movies = client.listMovies(server.apiKey, JellyfinServerService.selectedLibraryIds(server));
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach Jellyfin server: " + e.getMessage());
    }

    int linked = 0;
    int created = 0;
    int skipped = 0;
    int alreadySynced = 0;

    for (JellyfinMovie movie : movies) {
      if (movie.tmdbId() == null || movie.path() == null) {
        skipped++;
        continue;
      }
      try {
        String outcome =
            QuarkusTransaction.requiringNew().call(() -> syncOneMovie(serverId, movie));
        switch (outcome) {
          case "linked" -> linked++;
          case "created" -> created++;
          default -> alreadySynced++;
        }
      } catch (RuntimeException e) {
        alreadySynced++; // most likely a race on the same path; treat as already-handled
      }
    }

    List<JellyfinShow> shows;
    List<JellyfinEpisode> episodes;
    try {
      shows = client.listShows(server.apiKey, JellyfinServerService.selectedLibraryIds(server));
      episodes =
          client.listEpisodes(server.apiKey, JellyfinServerService.selectedLibraryIds(server));
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach Jellyfin server: " + e.getMessage());
    }
    Map<String, List<JellyfinEpisode>> episodesBySeriesId =
        episodes.stream()
            .filter(e -> e.seriesId() != null)
            .collect(Collectors.groupingBy(JellyfinEpisode::seriesId));

    int showsLinked = 0;
    int showsCreated = 0;
    int showsSkipped = 0;
    int showsAlreadySynced = 0;
    int episodeFilesLinked = 0;

    for (JellyfinShow show : shows) {
      if (show.tmdbId() == null || show.path() == null) {
        showsSkipped++;
        continue;
      }
      List<JellyfinEpisode> showEpisodes = episodesBySeriesId.getOrDefault(show.id(), List.of());
      try {
        ShowSyncOutcome outcome =
            QuarkusTransaction.requiringNew().call(() -> syncOneShow(show, showEpisodes));
        switch (outcome.outcome()) {
          case "linked" -> showsLinked++;
          case "created" -> showsCreated++;
          default -> showsAlreadySynced++;
        }
        episodeFilesLinked += outcome.episodeFilesLinked();
      } catch (RuntimeException e) {
        showsAlreadySynced++; // most likely a race, or this show's TMDB fetch failed; skip for now
      }
    }

    List<JellyfinUser> jellyfinUsers;
    try {
      jellyfinUsers = client.listUsers(server.apiKey);
    } catch (IOException | InterruptedException e) {
      jellyfinUsers = List.of();
    }

    int usersCreated = 0;
    int usersUpdated = 0;
    for (JellyfinUser jellyfinUser : jellyfinUsers) {
      try {
        String outcome =
            QuarkusTransaction.requiringNew().call(() -> syncOneUser(serverId, jellyfinUser));
        switch (outcome) {
          case "created" -> usersCreated++;
          case "updated" -> usersUpdated++;
          default -> {} // "unchanged" — not worth reporting
        }
      } catch (RuntimeException e) {
        // e.g. this account's display name collides with an existing native username —
        // must never abort syncing the rest of the users or any of the movies above.
      }
    }

    return new JellyfinSyncResult(
        movies.size(),
        linked,
        created,
        skipped,
        alreadySynced,
        shows.size(),
        showsLinked,
        showsCreated,
        showsSkipped,
        showsAlreadySynced,
        episodeFilesLinked,
        usersCreated,
        usersUpdated);
  }

  /**
   * @return "linked", "created", or "already-synced"
   */
  private String syncOneMovie(UUID serverId, JellyfinMovie movie) {
    Optional<LibraryFile> existingFile =
        LibraryFile.find("path", movie.path()).firstResultOptional();
    if (existingFile.isPresent()) {
      backfillIfIncomplete(existingFile.get().mediaItem, movie);
      return "already-synced";
    }

    Optional<MediaItemExternalId> existingLink =
        MediaItemExternalId.find(
                "plugin.slug = ?1 and externalId = ?2", TMDB_PLUGIN_SLUG, movie.tmdbId())
            .firstResultOptional();

    MediaItem mediaItem;
    String outcome;
    if (existingLink.isPresent()) {
      mediaItem = existingLink.get().mediaItem;
      outcome = "linked";
      backfillIfIncomplete(mediaItem, movie);
    } else {
      mediaItem = createMovie(movie);
      outcome = "created";
    }
    createLibraryFile(mediaItem, movie);
    return outcome;
  }

  /**
   * @return "linked", "created", or "already-synced" (mirrors {@link #syncOneMovie}) — plus how
   *     many new episode files got matched to this show's TMDB-built episode tree.
   */
  private ShowSyncOutcome syncOneShow(JellyfinShow show, List<JellyfinEpisode> episodes) {
    Optional<MediaItemExternalId> existingLink =
        MediaItemExternalId.find(
                "plugin.slug = ?1 and externalId = ?2 and mediaItem.contentType = 'show'"
                    + " and supersededAt is null",
                TMDB_PLUGIN_SLUG,
                show.tmdbId())
            .firstResultOptional();

    MediaItem mediaItem;
    boolean created;
    if (existingLink.isPresent()) {
      mediaItem = existingLink.get().mediaItem;
      created = false;
      if (mediaItem.rootFolder == null) {
        resolveRootFolder(show.path(), "show").ifPresent(folder -> mediaItem.rootFolder = folder);
      }
    } else {
      mediaItem = createShow(show);
      created = true;
    }

    int linkedFiles = linkEpisodeFiles(mediaItem, episodes);
    String outcome = created ? "created" : (linkedFiles > 0 ? "linked" : "already-synced");
    return new ShowSyncOutcome(outcome, linkedFiles);
  }

  private MediaItem createShow(JellyfinShow jellyfinShow) {
    LibraryRootFolder rootFolder = resolveRootFolder(jellyfinShow.path(), "show").orElse(null);
    Show show =
        showService.createFromJellyfin(
            jellyfinShow.name(), jellyfinShow.year(), jellyfinShow.tmdbId(), rootFolder);
    return show.mediaItem;
  }

  /**
   * Matches each Jellyfin episode file to the Kosmos {@link Episode} at the same (season, episode)
   * number under this show — Jellyfin's own episode-level ProviderIds aren't reliably populated, so
   * season/episode number is the only stable join key available. An episode Jellyfin has but TMDB's
   * tree doesn't (e.g. an unindexed special) is silently skipped rather than guessed at.
   *
   * <p>Deduplicates by path first: Jellyfin can list the same physical file twice for one show
   * (e.g. the same folder swept into two overlapping libraries) — without this, two entries for the
   * same path would both pass the "not already linked" check below (neither is flushed to the DB
   * yet, so the second can't see the first), and the second insert would fail the {@code
   * library_file.path} unique constraint and abort this show's whole transaction.
   */
  private int linkEpisodeFiles(MediaItem showMediaItem, List<JellyfinEpisode> jellyfinEpisodes) {
    if (jellyfinEpisodes.isEmpty()) {
      return 0;
    }
    Show show = Show.<Show>findByIdOptional(showMediaItem.id).orElse(null);
    if (show == null) {
      return 0;
    }

    Map<String, JellyfinEpisode> byPath = new LinkedHashMap<>();
    for (JellyfinEpisode jellyfinEpisode : jellyfinEpisodes) {
      if (jellyfinEpisode.path() != null) {
        byPath.putIfAbsent(jellyfinEpisode.path(), jellyfinEpisode);
      }
    }

    int linked = 0;
    for (JellyfinEpisode jellyfinEpisode : byPath.values()) {
      if (jellyfinEpisode.seasonNumber() == null
          || jellyfinEpisode.episodeNumber() == null
          || jellyfinEpisode.path() == null) {
        continue;
      }
      Optional<Episode> episode =
          Episode.<Episode>find(
                  "season.show = ?1 and season.seasonNumber = ?2 and episodeNumber = ?3",
                  show,
                  jellyfinEpisode.seasonNumber(),
                  jellyfinEpisode.episodeNumber())
              .firstResultOptional();
      if (episode.isEmpty()) {
        continue;
      }
      if (LibraryFile.find("path", jellyfinEpisode.path()).firstResultOptional().isPresent()) {
        continue;
      }

      LibraryFile file = new LibraryFile();
      file.mediaItem = episode.get().mediaItem;
      file.path = jellyfinEpisode.path();
      file.sizeBytes = sizeOrZero(jellyfinEpisode.path());
      file.matchMethod = MATCH_METHOD;
      file.matchConfidence = 1.0f;
      file.matchPinned = false;
      file.matchedAt = Instant.now();
      file.verified = false;
      file.importedAt = Instant.now();
      probeService.tryProbe(file);
      file.persist();
      linked++;
    }
    return linked;
  }

  private record ShowSyncOutcome(String outcome, int episodeFilesLinked) {}

  /**
   * Self-healing for rows created before poster/root-folder backfill existed here (or from a run
   * where the TMDB lookup itself failed) — every sync run gets another chance to fill in whatever a
   * previously-linked item is still missing, not just brand-new ones.
   */
  private void backfillIfIncomplete(MediaItem mediaItem, JellyfinMovie jellyfinMovie) {
    Movie movie = Movie.<Movie>findByIdOptional(mediaItem.id).orElse(null);
    if (movie != null && (movie.posterPath == null || movie.posterPath.isBlank())) {
      enrichFromTmdb(movie, jellyfinMovie.tmdbId());
    }
    if (mediaItem.rootFolder == null) {
      resolveRootFolder(jellyfinMovie.path(), "movie")
          .ifPresent(folder -> mediaItem.rootFolder = folder);
    }
  }

  /**
   * @return "created", "updated" (role or display name actually changed), or "unchanged"
   */
  private String syncOneUser(UUID serverId, JellyfinUser jellyfinUser) {
    JellyfinServer server = JellyfinServer.<JellyfinServer>findById(serverId);
    String role = jellyfinUser.isAdmin() ? "ADMIN" : "USER";
    Optional<User> existing =
        User.find("jellyfinServer = ?1 and jellyfinUserId = ?2", server, jellyfinUser.id())
            .firstResultOptional();

    if (existing.isPresent()) {
      User user = existing.get();
      boolean changed = !role.equals(user.role) || !jellyfinUser.name().equals(user.displayName);
      user.role = role;
      user.displayName = jellyfinUser.name();
      return changed ? "updated" : "unchanged";
    }

    User user = new User();
    user.username = jellyfinUser.name();
    user.displayName = jellyfinUser.name();
    user.jellyfinServer = server;
    user.jellyfinUserId = jellyfinUser.id();
    user.role = role;
    user.enabled = true;
    user.createdAt = Instant.now();
    user.persist();
    return "created";
  }

  private MediaItem createMovie(JellyfinMovie jellyfinMovie) {
    MediaItem mediaItem = new MediaItem();
    mediaItem.contentType = "movie";
    mediaItem.title = jellyfinMovie.name();
    mediaItem.year = jellyfinMovie.year();
    mediaItem.addedAt = Instant.now();
    resolveRootFolder(jellyfinMovie.path(), "movie")
        .ifPresent(folder -> mediaItem.rootFolder = folder);
    mediaItem.persist();

    Movie movie = new Movie();
    movie.mediaItem = mediaItem;
    movie.persist();
    enrichFromTmdb(movie, jellyfinMovie.tmdbId());

    Plugin plugin = findOrCreateTmdbPlugin();
    MediaItemExternalId link = new MediaItemExternalId();
    link.mediaItem = mediaItem;
    link.plugin = plugin;
    link.externalId = jellyfinMovie.tmdbId();
    link.matchedAt = Instant.now();
    link.persist();

    return mediaItem;
  }

  /**
   * Jellyfin's ProviderIds give a TMDB id but no artwork/overview of its own — one extra TMDB round
   * trip per movie (cached 7 days, see application.properties) backfills what Kosmos's own poster
   * rendering needs. Best-effort: a failed/unconfigured lookup leaves the movie exactly as bare as
   * it was before, never blocks the sync.
   */
  private void enrichFromTmdb(Movie movie, String tmdbId) {
    Optional<MetadataSearchResult> result = tmdbMetadataProvider.fetchMovieById(tmdbId);
    result.ifPresent(
        r -> {
          movie.posterPath = r.posterPath();
          movie.backdropPath = r.backdropPath();
          movie.overview = r.overview();
        });
  }

  /**
   * Prefers the registered root folder Jellyfin's own reported path actually falls under (see
   * {@link de.oppahansi.kosmos.library.LibraryRootFolderService#findContaining}) — e.g. a movie
   * under {@code /media/anime-movies} lands under that folder specifically, not just "the movies
   * default" — falling back to the {@code contentType} default only if none of the registered
   * folders actually contain this path.
   */
  private Optional<LibraryRootFolder> resolveRootFolder(String path, String contentType) {
    Optional<LibraryRootFolder> containing = rootFolderService.findContaining(path);
    return containing.isPresent() ? containing : rootFolderService.getDefault(contentType);
  }

  private void createLibraryFile(MediaItem mediaItem, JellyfinMovie jellyfinMovie) {
    LibraryFile file = new LibraryFile();
    file.mediaItem = mediaItem;
    file.path = jellyfinMovie.path();
    file.sizeBytes = sizeOrZero(jellyfinMovie.path());
    file.matchMethod = MATCH_METHOD;
    file.matchConfidence = 1.0f;
    file.matchPinned = false;
    file.matchedAt = Instant.now();
    file.verified = false;
    file.importedAt = Instant.now();
    probeService.tryProbe(file); // best-effort — only succeeds if Kosmos can see this path too
    file.persist();
  }

  private long sizeOrZero(String path) {
    try {
      return Files.size(Path.of(path));
    } catch (Exception e) {
      return 0L;
    }
  }

  private Plugin findOrCreateTmdbPlugin() {
    return Plugin.<Plugin>find("slug", TMDB_PLUGIN_SLUG)
        .firstResultOptional()
        .orElseGet(
            () -> {
              Plugin plugin = new Plugin();
              plugin.slug = TMDB_PLUGIN_SLUG;
              plugin.name = "TMDB";
              plugin.kind = "metadata";
              plugin.builtIn = true;
              plugin.enabled = true;
              plugin.installedAt = Instant.now();
              plugin.persist();
              return plugin;
            });
  }
}
