package de.oppahansi.kosmos.library.naming;

import de.oppahansi.kosmos.library.naming.dto.UpdateNamingSettingsRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes {@link NamingSettings}, one row per content type. No row is required to exist up
 * front — {@link #forContentType} always returns something usable, backed by {@link #DEFAULTS} (the
 * exact shapes {@code ImportService} hardcoded before this existed), so a fresh instance or one
 * content type nobody has customized just works.
 */
@ApplicationScoped
public class NamingSettingsService {

  public static final List<String> CONTENT_TYPES = List.of("movie", "show", "anime");

  private static final Map<String, NamingSettings> DEFAULTS = buildDefaults();

  /**
   * Every content type's effective settings — the persisted row where one exists, the built-in
   * default otherwise. What the settings page lists and edits.
   */
  public List<NamingSettings> listEffective() {
    return CONTENT_TYPES.stream().map(this::forContentType).toList();
  }

  public NamingSettings forContentType(String contentType) {
    return NamingSettings.<NamingSettings>find("contentType", contentType)
        .firstResultOptional()
        .orElseGet(() -> DEFAULTS.get(contentType));
  }

  @Transactional
  public NamingSettings update(String contentType, UpdateNamingSettingsRequest request) {
    if (!CONTENT_TYPES.contains(contentType)) {
      throw new BadRequestException("Unknown content type: " + contentType);
    }
    NamingSettings settings =
        NamingSettings.<NamingSettings>find("contentType", contentType)
            .firstResultOptional()
            .orElseGet(
                () -> {
                  NamingSettings created = new NamingSettings();
                  created.contentType = contentType;
                  return created;
                });
    settings.folderTemplate = request.folderTemplate();
    settings.fileTemplate = request.fileTemplate();
    settings.seasonFolderTemplate = request.seasonFolderTemplate();
    if (settings.id == null) {
      settings.persist();
    }
    return settings;
  }

  private static Map<String, NamingSettings> buildDefaults() {
    Map<String, NamingSettings> defaults = new LinkedHashMap<>();
    defaults.put("movie", defaultSettings("movie", "{Title} ({Year})", "{Title} ({Year})", null));
    defaults.put(
        "show",
        defaultSettings(
            "show",
            "{Title} ({Year})",
            "{Title} - S{Season:00}E{Episode:00} - {EpisodeTitle}",
            "Season {Season:00}"));
    defaults.put(
        "anime",
        defaultSettings(
            "anime", "{Title} ({Year})", "{Title} - {Absolute:00} - {EpisodeTitle}", null));
    return defaults;
  }

  private static NamingSettings defaultSettings(
      String contentType, String folderTemplate, String fileTemplate, String seasonFolderTemplate) {
    NamingSettings settings = new NamingSettings();
    settings.contentType = contentType;
    settings.folderTemplate = folderTemplate;
    settings.fileTemplate = fileTemplate;
    settings.seasonFolderTemplate = seasonFolderTemplate;
    return settings;
  }
}
