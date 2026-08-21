package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.library.dto.CreateLibraryRootFolderRequest;
import de.oppahansi.kosmos.library.dto.LibraryRootFolderResponse;
import de.oppahansi.kosmos.library.dto.LibraryStatsResponse;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import java.util.UUID;

@Path("/library")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LibraryResource {

  @Inject LibraryRootFolderService rootFolderService;

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
    Long totalBytes =
        rootFolderService.getDefault().map(f -> totalSpaceOrNull(f.path)).orElse(null);
    return new LibraryStatsResponse(movieCount, seriesCount, animeCount, usedBytes, totalBytes);
  }

  @GET
  @Path("/root-folders")
  public List<LibraryRootFolderResponse> listRootFolders() {
    return rootFolderService.listAll().stream().map(LibraryRootFolderResponse::from).toList();
  }

  @POST
  @Path("/root-folders")
  public Response createRootFolder(CreateLibraryRootFolderRequest request) {
    LibraryRootFolderResponse response =
        LibraryRootFolderResponse.from(
            rootFolderService.create(request.path(), request.contentTypes()));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @DELETE
  @Path("/root-folders/{id}")
  public Response deleteRootFolder(@PathParam("id") UUID id) {
    rootFolderService.delete(id);
    return Response.noContent().build();
  }

  private Long totalSpaceOrNull(String rootPath) {
    File dir = new File(rootPath);
    return dir.exists() ? Long.valueOf(dir.getTotalSpace()) : null;
  }
}
