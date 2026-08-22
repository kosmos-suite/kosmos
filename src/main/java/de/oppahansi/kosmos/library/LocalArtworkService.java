package de.oppahansi.kosmos.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.library.naming.NamingContext;
import de.oppahansi.kosmos.library.naming.NamingSettings;
import de.oppahansi.kosmos.library.naming.NamingSettingsService;
import de.oppahansi.kosmos.library.naming.NamingTemplateEngine;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Local-disk artwork fallback for titles TMDB has no poster/backdrop for: a sidecar image file (the
 * convention Jellyfin/Kodi/Plex-managed folders already use) next to the title's own directory, or
 * — poster only, movies only, where a single video file stands in for the whole item — an embedded
 * cover-art stream extracted with ffmpeg. Best-effort throughout: any failure just means no local
 * artwork, never an error surfaced to the caller.
 */
@ApplicationScoped
public class LocalArtworkService {

  public enum Kind {
    POSTER(
        List.of("poster.jpg", "poster.png", "folder.jpg", "folder.png", "cover.jpg", "cover.png"),
        true),
    BACKDROP(List.of("backdrop.jpg", "backdrop.png", "fanart.jpg", "fanart.png"), false);

    private final List<String> sidecarNames;
    private final boolean embeddedFallback;

    Kind(List<String> sidecarNames, boolean embeddedFallback) {
      this.sidecarNames = sidecarNames;
      this.embeddedFallback = embeddedFallback;
    }
  }

  @Inject ObjectMapper objectMapper;
  @Inject NamingSettingsService namingSettingsService;

  private final NamingTemplateEngine namingTemplateEngine = new NamingTemplateEngine();

  public record Artwork(byte[] bytes, String contentType) {}

  public Optional<Artwork> find(MediaItem mediaItem, Kind kind) {
    Optional<Artwork> sidecar = itemDirectory(mediaItem).flatMap(dir -> findSidecar(dir, kind));
    if (sidecar.isPresent()) {
      return sidecar;
    }
    return kind.embeddedFallback && "movie".equals(mediaItem.contentType)
        ? findEmbedded(mediaItem)
        : Optional.empty();
  }

  /**
   * Same folder {@link de.oppahansi.kosmos.library.ImportService} just wrote (or would write) this
   * item under — has to stay in sync with {@link NamingSettingsService}'s templates, not a separate
   * hardcoded guess, or a customized naming scheme would silently break local-artwork lookup.
   */
  private Optional<Path> itemDirectory(MediaItem mediaItem) {
    if (mediaItem.rootFolder == null
        || !NamingSettingsService.CONTENT_TYPES.contains(mediaItem.contentType)) {
      return Optional.empty();
    }
    NamingSettings settings = namingSettingsService.forContentType(mediaItem.contentType);
    NamingContext context = NamingContext.forMovie(mediaItem.title, mediaItem.year);
    String folderName = namingTemplateEngine.render(settings.folderTemplate, context);
    Path dir = Path.of(mediaItem.rootFolder.path, folderName);
    return Files.isDirectory(dir) ? Optional.of(dir) : Optional.empty();
  }

  private Optional<Artwork> findSidecar(Path dir, Kind kind) {
    for (String name : kind.sidecarNames) {
      Path candidate = dir.resolve(name);
      if (Files.isRegularFile(candidate)) {
        try {
          return Optional.of(new Artwork(Files.readAllBytes(candidate), contentTypeFor(name)));
        } catch (IOException e) {
          // Try the next sidecar name rather than failing the whole lookup.
        }
      }
    }
    return Optional.empty();
  }

  private String contentTypeFor(String filename) {
    return filename.endsWith(".png") ? "image/png" : "image/jpeg";
  }

  private Optional<Artwork> findEmbedded(MediaItem mediaItem) {
    LibraryFile file = LibraryFile.find("mediaItem", mediaItem).firstResult();
    if (file == null) {
      return Optional.empty();
    }
    return attachedPicStream(file.path).flatMap(stream -> extractFrame(file.path, stream));
  }

  private record AttachedPicStream(int index, String codecName) {}

  private Optional<AttachedPicStream> attachedPicStream(String videoPath) {
    ProcessBuilder builder =
        new ProcessBuilder(
            "ffprobe", "-v", "quiet", "-print_format", "json", "-show_streams", videoPath);
    builder.redirectErrorStream(false);
    try {
      Process process = builder.start();
      JsonNode root = objectMapper.readTree(process.getInputStream());
      boolean finished = process.waitFor(15, TimeUnit.SECONDS);
      if (!finished || process.exitValue() != 0) {
        return Optional.empty();
      }
      for (JsonNode stream : root.path("streams")) {
        boolean isVideo = "video".equals(stream.path("codec_type").asText());
        boolean isAttachedPic = stream.path("disposition").path("attached_pic").asInt(0) == 1;
        if (isVideo && isAttachedPic) {
          return Optional.of(
              new AttachedPicStream(
                  stream.path("index").asInt(), stream.path("codec_name").asText("")));
        }
      }
      return Optional.empty();
    } catch (IOException | InterruptedException e) {
      return Optional.empty();
    }
  }

  private Optional<Artwork> extractFrame(String videoPath, AttachedPicStream stream) {
    ProcessBuilder builder =
        new ProcessBuilder(
            "ffmpeg",
            "-y",
            "-i",
            videoPath,
            "-map",
            "0:" + stream.index(),
            "-c",
            "copy",
            "-f",
            "image2pipe",
            "-vframes",
            "1",
            "pipe:1");
    try {
      Process process = builder.start();
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      process.getInputStream().transferTo(buffer);
      boolean finished = process.waitFor(20, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        return Optional.empty();
      }
      byte[] bytes = buffer.toByteArray();
      if (process.exitValue() != 0 || bytes.length == 0) {
        return Optional.empty();
      }
      return Optional.of(
          new Artwork(bytes, "png".equals(stream.codecName()) ? "image/png" : "image/jpeg"));
    } catch (IOException | InterruptedException e) {
      return Optional.empty();
    }
  }
}
