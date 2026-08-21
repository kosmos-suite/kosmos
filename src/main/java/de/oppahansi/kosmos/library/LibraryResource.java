package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.library.dto.LibraryStatsResponse;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/library")
@Produces(MediaType.APPLICATION_JSON)
public class LibraryResource {

  @ConfigProperty(name = "kosmos.library.root-path")
  Optional<String> libraryRootPath;

  @GET
  @Path("/stats")
  public LibraryStatsResponse stats() {
    long movieCount = MediaItem.count("contentType = ?1", "movie");
    long seriesCount = MediaItem.count("contentType = ?1", "show");
    long animeCount = MediaItem.count("contentType = ?1", "anime");
    long usedBytes =
        LibraryFile.getEntityManager()
            .createQuery("select coalesce(sum(f.sizeBytes), 0) from LibraryFile f", Long.class)
            .getSingleResult();
    Long totalBytes = libraryRootPath.map(this::totalSpaceOrNull).orElse(null);
    return new LibraryStatsResponse(movieCount, seriesCount, animeCount, usedBytes, totalBytes);
  }

  private Long totalSpaceOrNull(String rootPath) {
    File dir = new File(rootPath);
    return dir.exists() ? Long.valueOf(dir.getTotalSpace()) : null;
  }
}
