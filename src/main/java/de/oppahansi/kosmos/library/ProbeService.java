package de.oppahansi.kosmos.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.VideoCodec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Probes a {@link LibraryFile} on disk with ffprobe, populating its real stream attributes. */
@ApplicationScoped
public class ProbeService {

  private static final Pattern BIT_DEPTH = Pattern.compile("(\\d+)(?:le|be)$");

  @Inject ObjectMapper objectMapper;

  /**
   * Throws on failure — for the explicit re-probe endpoint, where a definitive answer is expected.
   */
  public void probe(LibraryFile file) {
    JsonNode root = runFfprobe(file.path);
    JsonNode format = root.path("format");
    JsonNode video = firstVideoStream(root);

    file.container = firstToken(format.path("format_name").asText(null));
    file.durationSeconds = parseWholeSeconds(format.path("duration").asText(null));
    file.embeddedTitle = nonBlankOrNull(format.path("tags").path("title").asText(null));

    if (video != null) {
      file.videoCodec = canonicalVideoCodec(video.path("codec_name").asText(null));
      file.resolutionWidth = intOrNull(video, "width");
      file.resolutionHeight = intOrNull(video, "height");
      file.bitDepth = parseBitDepth(video.path("pix_fmt").asText(null));
      file.hdrFormat = detectHdr(video);
      if (file.durationSeconds == null) {
        file.durationSeconds = parseWholeSeconds(video.path("duration").asText(null));
      }
    }

    file.probedAt = Instant.now();
    file.verified = true;
  }

  /**
   * Best-effort — swallows failures so a probing problem never blocks an otherwise-successful
   * import.
   */
  public void tryProbe(LibraryFile file) {
    try {
      probe(file);
    } catch (RuntimeException e) {
      // Left unverified/unprobed; the explicit re-probe endpoint can retry and will surface the
      // error.
    }
  }

  private JsonNode runFfprobe(String path) {
    ProcessBuilder builder =
        new ProcessBuilder(
            "ffprobe",
            "-v",
            "quiet",
            "-print_format",
            "json",
            "-show_format",
            "-show_streams",
            path);
    builder.redirectErrorStream(false);
    try {
      Process process = builder.start();
      JsonNode result = objectMapper.readTree(process.getInputStream());
      boolean finished = process.waitFor(30, TimeUnit.SECONDS);
      if (!finished || process.exitValue() != 0) {
        throw probeFailed(path, "ffprobe exited abnormally or timed out");
      }
      return result;
    } catch (IOException e) {
      throw probeFailed(path, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw probeFailed(path, "interrupted");
    }
  }

  private JsonNode firstVideoStream(JsonNode root) {
    for (JsonNode stream : root.path("streams")) {
      if ("video".equals(stream.path("codec_type").asText())) {
        return stream;
      }
    }
    return null;
  }

  private String canonicalVideoCodec(String ffprobeCodecName) {
    if (ffprobeCodecName == null) {
      return null;
    }
    for (VideoCodec codec : VideoCodec.values()) {
      if (codec.pattern().matcher(ffprobeCodecName).find()) {
        return codec.canonicalName();
      }
    }
    return ffprobeCodecName;
  }

  private Integer parseWholeSeconds(String seconds) {
    if (seconds == null) {
      return null;
    }
    try {
      return (int) Math.round(Double.parseDouble(seconds));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseBitDepth(String pixFmt) {
    if (pixFmt == null) {
      return 8;
    }
    Matcher matcher = BIT_DEPTH.matcher(pixFmt);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 8;
  }

  private String detectHdr(JsonNode video) {
    String transfer = video.path("color_transfer").asText("");
    return switch (transfer) {
      case "smpte2084" -> "HDR10";
      case "arib-std-b67" -> "HLG";
      default -> null;
    };
  }

  private Integer intOrNull(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.path(field).asInt() : null;
  }

  private String firstToken(String commaSeparated) {
    if (commaSeparated == null) {
      return null;
    }
    int comma = commaSeparated.indexOf(',');
    return comma < 0 ? commaSeparated : commaSeparated.substring(0, comma);
  }

  private String nonBlankOrNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private WebApplicationException probeFailed(String path, String reason) {
    return new WebApplicationException(
        "Could not probe " + path + ": " + reason, Response.Status.BAD_GATEWAY);
  }
}
