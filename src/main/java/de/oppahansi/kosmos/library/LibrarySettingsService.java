package de.oppahansi.kosmos.library;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * kosmos.library.root-path can come from either the runtime-set {@link LibrarySetting} row or the
 * kosmos.library.root-path config property (env var) — the row wins when both are present.
 */
@ApplicationScoped
public class LibrarySettingsService {

  @ConfigProperty(name = "kosmos.library.root-path")
  Optional<String> configuredRootPath;

  public Optional<String> getRootPath() {
    Optional<String> stored = storedRootPath();
    return stored.isPresent() ? stored : configuredRootPath;
  }

  public String getSource() {
    if (storedRootPath().isPresent()) {
      return "runtime";
    }
    return configuredRootPath.isPresent() ? "env" : "unset";
  }

  @Transactional
  public void setRootPath(String rootPath) {
    LibrarySetting setting =
        LibrarySetting.<LibrarySetting>findAll()
            .firstResultOptional()
            .orElseGet(LibrarySetting::new);
    setting.rootPath = rootPath;
    if (setting.id == null) {
      setting.persist();
    }
  }

  private Optional<String> storedRootPath() {
    return LibrarySetting.<LibrarySetting>findAll()
        .firstResultOptional()
        .map(setting -> setting.rootPath)
        .filter(path -> path != null && !path.isBlank());
  }
}
