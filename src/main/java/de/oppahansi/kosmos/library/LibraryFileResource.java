package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.library.dto.LibraryFileResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/library-files")
@Produces(MediaType.APPLICATION_JSON)
public class LibraryFileResource {

  @Inject ProbeService probeService;

  @POST
  @Path("/{id}/probe")
  @Transactional
  public Response probe(@PathParam("id") UUID id) {
    return LibraryFile.<LibraryFile>findByIdOptional(id)
        .map(
            file -> {
              probeService.probe(file);
              return Response.ok(LibraryFileResponse.from(file)).build();
            })
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }
}
