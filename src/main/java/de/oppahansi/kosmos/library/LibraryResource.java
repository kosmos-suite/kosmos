package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.library.dto.LibraryRootPathRequest;
import de.oppahansi.kosmos.library.dto.LibraryRootPathResponse;
import de.oppahansi.kosmos.library.dto.LibraryStatsResponse;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;

@Path("/library")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LibraryResource {

  @Inject LibrarySettingsService librarySettingsService;

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
    Long totalBytes = librarySettingsService.getRootPath().map(this::totalSpaceOrNull).orElse(null);
    return new LibraryStatsResponse(movieCount, seriesCount, animeCount, usedBytes, totalBytes);
  }

  @GET
  @Path("/root-path")
  public LibraryRootPathResponse getRootPath() {
    return new LibraryRootPathResponse(
        librarySettingsService.getRootPath().orElse(null), librarySettingsService.getSource());
  }

  @PUT
  @Path("/root-path")
  public Response setRootPath(LibraryRootPathRequest request) {
    librarySettingsService.setRootPath(request.rootPath());
    return Response.noContent().build();
  }

  private Long totalSpaceOrNull(String rootPath) {
    File dir = new File(rootPath);
    return dir.exists() ? Long.valueOf(dir.getTotalSpace()) : null;
  }
}
