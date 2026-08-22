package de.oppahansi.kosmos.health;

import de.oppahansi.kosmos.library.LibraryRootFolderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** One result per content type with no root folder it could land in — an import would fail. */
@ApplicationScoped
public class RootFolderHealthCheck implements HealthCheck {

  private static final List<String> CONTENT_TYPES = List.of("movie", "show", "anime");

  @Inject LibraryRootFolderService rootFolderService;

  @Override
  public HealthCheckResult check() {
    List<String> missing =
        CONTENT_TYPES.stream().filter(type -> !rootFolderService.hasFolderAccepting(type)).toList();
    if (missing.isEmpty()) {
      return HealthCheckResult.ok("Root Folders");
    }
    return HealthCheckResult.warning(
        "Root Folders", "No root folder accepts: " + String.join(", ", missing) + ".");
  }
}
