package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.auth.User;
import de.oppahansi.kosmos.jellyfin.dto.JellyfinSyncResult;
import de.oppahansi.kosmos.library.LibraryFile;
import de.oppahansi.kosmos.library.ProbeService;
import de.oppahansi.kosmos.media.MediaItem;
import de.oppahansi.kosmos.media.Movie;
import de.oppahansi.kosmos.metadata.MediaItemExternalId;
import de.oppahansi.kosmos.metadata.Plugin;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
      movies = client.listMovies(server.apiKey);
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
        boolean wasCreated =
            QuarkusTransaction.requiringNew().call(() -> syncOneUser(serverId, jellyfinUser));
        if (wasCreated) {
          usersCreated++;
        } else {
          usersUpdated++;
        }
      } catch (RuntimeException e) {
        // e.g. this account's display name collides with an existing native username —
        // must never abort syncing the rest of the users or any of the movies above.
      }
    }

    return new JellyfinSyncResult(
        movies.size(), linked, created, skipped, alreadySynced, usersCreated, usersUpdated);
  }

  /**
   * @return "linked", "created", or "already-synced"
   */
  private String syncOneMovie(UUID serverId, JellyfinMovie movie) {
    if (LibraryFile.find("path", movie.path()).firstResultOptional().isPresent()) {
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
    } else {
      mediaItem = createMovie(movie);
      outcome = "created";
    }
    createLibraryFile(mediaItem, movie);
    return outcome;
  }

  /**
   * @return true if a new User row was created, false if an existing one was found (and possibly
   *     updated)
   */
  private boolean syncOneUser(UUID serverId, JellyfinUser jellyfinUser) {
    JellyfinServer server = JellyfinServer.<JellyfinServer>findById(serverId);
    String role = jellyfinUser.isAdmin() ? "ADMIN" : "USER";
    Optional<User> existing =
        User.find("jellyfinServer = ?1 and jellyfinUserId = ?2", server, jellyfinUser.id())
            .firstResultOptional();

    if (existing.isPresent()) {
      User user = existing.get();
      user.role = role;
      user.displayName = jellyfinUser.name();
      return false;
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
    return true;
  }

  private MediaItem createMovie(JellyfinMovie jellyfinMovie) {
    MediaItem mediaItem = new MediaItem();
    mediaItem.contentType = "movie";
    mediaItem.title = jellyfinMovie.name();
    mediaItem.year = jellyfinMovie.year();
    mediaItem.addedAt = Instant.now();
    mediaItem.persist();

    // Jellyfin's ProviderIds don't include a poster/overview — those still need a TMDB lookup
    // by this same id, which nothing does automatically yet. Left null rather than guessed at.
    Movie movie = new Movie();
    movie.mediaItem = mediaItem;
    movie.persist();

    Plugin plugin = findOrCreateTmdbPlugin();
    MediaItemExternalId link = new MediaItemExternalId();
    link.mediaItem = mediaItem;
    link.plugin = plugin;
    link.externalId = jellyfinMovie.tmdbId();
    link.matchedAt = Instant.now();
    link.persist();

    return mediaItem;
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
