package de.oppahansi.kosmos.library;

import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds importable video files under a path — a single file, or every matching file under a
 * directory (recursive, since a season-pack folder nests episodes one level down). Shared by {@link
 * ImportService} (picks the single largest — its historical, still-current behavior for a
 * one-file-at-a-time import) and {@code ImportMatchService} (wants every candidate, for the bulk
 * manual-import review screen). Pure and stateless: no CDI dependency, so it's usable from either
 * without an injection point.
 */
public final class VideoFileScanner {

  private static final Set<String> VIDEO_EXTENSIONS =
      Set.of("mp4", "mkv", "avi", "mov", "m4v", "ts", "wmv", "flv", "webm");
  private static final long SAMPLE_SIZE_THRESHOLD_BYTES = 50L * 1024 * 1024;
  private static final Pattern SAMPLE_NAME = Pattern.compile("(?i)\\bsample\\b");

  private VideoFileScanner() {}

  /**
   * Every importable video file under {@code source} — itself if it's already a file, or every
   * matching file found walking a directory. Empty (not an error) if a directory has none.
   */
  public static List<Path> findAll(Path source) {
    if (!Files.exists(source)) {
      throw new BadRequestException("Source path does not exist: " + source);
    }
    if (Files.isRegularFile(source)) {
      return isImportableVideo(source) ? List.of(source) : List.of();
    }
    try (Stream<Path> walk = Files.walk(source)) {
      return walk.filter(Files::isRegularFile).filter(VideoFileScanner::isImportableVideo).toList();
    } catch (IOException e) {
      throw new BadRequestException("Could not read source path: " + source);
    }
  }

  /**
   * The single largest importable video under {@code source} — {@link ImportService}'s
   * one-file-at-a-time pick, for when a directory is known to hold just one real release.
   */
  public static Path largest(Path source) {
    List<Path> all = findAll(source);
    return all.stream()
        .max(Comparator.comparingLong(VideoFileScanner::sizeOrZero))
        .orElseThrow(
            () -> new BadRequestException("No importable video file found under: " + source));
  }

  public static boolean isImportableVideo(Path path) {
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

  private static long sizeOrZero(Path path) {
    try {
      return Files.size(path);
    } catch (IOException e) {
      return 0L;
    }
  }
}
