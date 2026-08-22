package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.downloads.dto.BlocklistResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/blocklist")
@Produces(MediaType.APPLICATION_JSON)
public class BlocklistResource {

  @Inject BlocklistService blocklistService;

  @GET
  public List<BlocklistResponse> list() {
    return blocklistService.listAll().stream().map(BlocklistResponse::from).toList();
  }

  /**
   * Removes a blocklist entry so its release can be grabbed again — the manual retry escape hatch.
   */
  @DELETE
  @Path("/{id}")
  public Response remove(@PathParam("id") UUID id) {
    return blocklistService.remove(id)
        ? Response.noContent().build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }
}
