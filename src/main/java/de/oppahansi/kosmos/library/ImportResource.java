package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.library.dto.ImportRequest;
import de.oppahansi.kosmos.library.dto.LibraryFileResponse;
import de.oppahansi.kosmos.media.MediaItem;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;

@Path("/movies/{movieId}/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportResource {

  @Inject ImportService importService;

  @POST
  public Response importFile(@PathParam("movieId") UUID movieId, ImportRequest request) {
    Optional<MediaItem> mediaItem = MediaItem.<MediaItem>findByIdOptional(movieId);
    if (mediaItem.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    LibraryFile file = importService.importPath(mediaItem.get(), request.sourcePath());
    return Response.status(Response.Status.CREATED).entity(LibraryFileResponse.from(file)).build();
  }
}
