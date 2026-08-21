package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.media.MediaItem;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.anidb.AniDbUdpClient;
import de.oppahansi.kosmos.notifications.MovieImportedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Imports a completed download's video file into the library: hardlink first so the source torrent
 * keeps seeding untouched, falling back to copy only when source and target aren't on the same
 * filesystem; write under a temp name and atomically rename into place; reject samples and pick the
 * largest video file when given a whole torrent directory rather than a single file.
 */
@ApplicationScoped
public class ImportService {

  private static final Set<String> VIDEO_EXTENSIONS =
      Set.of("mp4", "mkv", "avi", "mov", "m4v", "ts", "wmv", "flv", "webm");
  private static final long SAMPLE_SIZE_THRESHOLD_BYTES = 50L * 1024 * 1024;
  private static final Pattern SAMPLE_NAME = Pattern.compile("(?i)\\bsample\\b");
  private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

  @ConfigProperty(name = "kosmos.library.root-path")
  Optional<String> libraryRootPath;

  @Inject ProbeService probeService;
  @Inject AniDbUdpClient aniDbUdpClient;
  @Inject ExternalIdLinkService externalIdLinkService;
  @Inject Event<MovieImportedEvent> movieImportedEvent;

  @Transactional
  public LibraryFile importPath(MediaItem mediaItem, String sourcePathRaw) {
    Path source = pickVideoFile(Path.of(sourcePathRaw));
    Path target = targetPathFor(mediaItem, source);

    if (LibraryFile.find("path", target.toString()).firstResultOptional().isPresent()) {
      throw new BadRequestException("Already imported at " + target);
    }

    materialize(source, target);

    LibraryFile file = new LibraryFile();
    file.mediaItem = mediaItem;
    file.path = target.toString();
    try {
      file.sizeBytes = Files.size(target);
    } catch (IOException e) {
      throw new InternalServerErrorException(
          "Imported file vanished immediately after import: " + target);
    }
    file.matchMethod = "GRAB";
    file.matchConfidence = 1.0f;
    file.matchPinned = false;
    file.matchedAt = Instant.now();
    file.verified = false;
    file.importedAt = Instant.now();
    probeService.tryProbe(file);
    file.persist();
    tryHash(file, target);
    movieImportedEvent.fire(new MovieImportedEvent(mediaItem.title, mediaItem.year));
    return file;
  }

  /**
   * Best-effort, same reasoning as {@link ProbeService#tryProbe} — a hashing failure (an unreadable
   * file, a full disk mid-read) shouldn't undo an otherwise-successful import. Every import gets
   * both hashes computed regardless of content type, since only the AniDB lookup that follows is
   * anime-specific, not the hashing itself.
   */
  private void tryHash(LibraryFile file, Path target) {
    String ed2k;
    try {
      persistHash(file, "CRC32", HashService.computeCrc32(target));
      ed2k = HashService.computeEd2k(target);
      persistHash(file, "ED2K", ed2k);
    } catch (IOException e) {
      // Left unhashed; nothing currently retries this, same gap as an unprobed file.
      return;
    }
    tryAniDbLookup(file, ed2k);
  }

  /**
   * AniDB hash identification — only attempted for anime episodes, since that's the one content
   * type AniDB indexes, and only when credentials are configured; skipped entirely otherwise rather
   * than throwing. A match upgrades {@code matchMethod} to record that this file's identity is
   * hash-verified rather than just inherited from whatever it was grabbed against, and links the
   * episode's own {@link MediaItem} to its AniDB episode id — {@code AnimeService} links the
   * series-level id separately, from Fribb's cross-reference data.
   */
  private void tryAniDbLookup(LibraryFile file, String ed2kHex) {
    if (!"anime_episode".equals(file.mediaItem.contentType) || !aniDbUdpClient.isConfigured()) {
      return;
    }
    try {
      aniDbUdpClient
          .lookupByHash(file.sizeBytes, ed2kHex)
          .ifPresent(
              match -> {
                externalIdLinkService.link(file.mediaItem, "anidb", match.episodeId());
                file.matchMethod = "ANIDB_HASH";
              });
    } catch (RuntimeException e) {
      // Best-effort — a lookup/ban/network failure shouldn't undo an otherwise-successful import.
    }
  }

  private void persistHash(LibraryFile file, String algorithm, String value) {
    LibraryFileHash hash = new LibraryFileHash();
    hash.libraryFile = file;
    hash.algorithm = algorithm;
    hash.value = value;
    hash.persist();
  }

  /**
   * Resolves a source path to the single video file to import — itself if a file, or the largest
   * non-sample video file if a directory.
   */
  private Path pickVideoFile(Path source) {
    if (!Files.exists(source)) {
      throw new BadRequestException("Source path does not exist: " + source);
    }
    if (Files.isRegularFile(source)) {
      if (!isImportableVideo(source)) {
        throw new BadRequestException("Not an importable video file: " + source);
      }
      return source;
    }

    try (Stream<Path> walk = Files.walk(source)) {
      return walk.filter(Files::isRegularFile)
          .filter(this::isImportableVideo)
          .max(Comparator.comparingLong(this::sizeOrZero))
          .orElseThrow(
              () -> new BadRequestException("No importable video file found under: " + source));
    } catch (IOException e) {
      throw new BadRequestException("Could not read source path: " + source);
    }
  }

  private boolean isImportableVideo(Path path) {
    String name = path.getFileName().toString();
    int dot = name.lastIndexOf('.');
    if (dot < 0 || !VIDEO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase())) {
      return false;
    }
    if (SAMPLE_NAME.matcher(name).find()) {
      return false;
    }
    return sizeOrZero(path) >= SAMPLE_SIZE_THRESHOLD_BYTES;
  }

  private long sizeOrZero(Path path) {
    try {
      return Files.size(path);
    } catch (IOException e) {
      return 0L;
    }
  }

  private Path targetPathFor(MediaItem mediaItem, Path source) {
    if (libraryRootPath.isEmpty()) {
      throw new InternalServerErrorException("kosmos.library.root-path is not configured");
    }
    String extension = "";
    String sourceName = source.getFileName().toString();
    int dot = sourceName.lastIndexOf('.');
    if (dot >= 0) {
      extension = sourceName.substring(dot);
    }

    String folderName = sanitize(mediaItem.title) + " (" + mediaItem.year + ")";
    return Path.of(libraryRootPath.get(), folderName, folderName + extension);
  }

  private String sanitize(String name) {
    return ILLEGAL_FILENAME_CHARS.matcher(name).replaceAll("").trim();
  }

  /**
   * Hardlinks source into a temp name beside target, falling back to copy across filesystems, then
   * atomically renames into place.
   */
  private void materialize(Path source, Path target) {
    try {
      Files.createDirectories(target.getParent());
      Path temp = target.resolveSibling(target.getFileName() + ".importing");
      Files.deleteIfExists(temp);

      try {
        Files.createLink(temp, source);
      } catch (IOException e) {
        // Not on the same filesystem (or hardlinks unsupported here) — fall back to copy.
        Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
      }

      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new InternalServerErrorException(
          "Failed to import " + source + " -> " + target + ": " + e.getMessage());
    }
  }
}
