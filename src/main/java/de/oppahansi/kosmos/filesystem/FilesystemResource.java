package de.oppahansi.kosmos.filesystem;

import de.oppahansi.kosmos.filesystem.dto.BrowseResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/filesystem")
@Produces(MediaType.APPLICATION_JSON)
public class FilesystemResource {

  @Inject FilesystemService filesystemService;

  @GET
  @Path("/browse")
  public BrowseResponse browse(@QueryParam("path") String path) {
    return filesystemService.browse(path);
  }
}
