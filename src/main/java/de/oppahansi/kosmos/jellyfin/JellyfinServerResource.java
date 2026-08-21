package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.jellyfin.dto.CreateJellyfinServerRequest;
import de.oppahansi.kosmos.jellyfin.dto.JellyfinLibraryResponse;
import de.oppahansi.kosmos.jellyfin.dto.JellyfinServerResponse;
import de.oppahansi.kosmos.jellyfin.dto.JellyfinSyncResult;
import de.oppahansi.kosmos.jellyfin.dto.RootFolderAutoRegisterResult;
import de.oppahansi.kosmos.jellyfin.dto.UpdateJellyfinLibrariesRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/jellyfin-servers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JellyfinServerResource {

  @Inject JellyfinServerService serverService;
  @Inject JellyfinSyncService syncService;

  @GET
  public List<JellyfinServerResponse> list() {
    return serverService.listAll().stream().map(JellyfinServerResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return serverService
        .findById(id)
        .map(server -> Response.ok(JellyfinServerResponse.from(server)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(CreateJellyfinServerRequest request) {
    JellyfinServerResponse response = JellyfinServerResponse.from(serverService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @POST
  @Path("/{id}/sync")
  public Response sync(@PathParam("id") UUID id) {
    JellyfinSyncResult result = syncService.sync(id);
    return Response.ok(result).build();
  }

  @GET
  @Path("/{id}/libraries")
  public List<JellyfinLibraryResponse> listLibraries(@PathParam("id") UUID id) {
    return serverService.listLibraries(id).stream().map(JellyfinLibraryResponse::from).toList();
  }

  @PUT
  @Path("/{id}/libraries")
  public Response updateLibraries(
      @PathParam("id") UUID id, UpdateJellyfinLibrariesRequest request) {
    serverService.updateSelectedLibraries(id, request.libraryIds());
    return Response.noContent().build();
  }

  @POST
  @Path("/{id}/root-folders")
  public RootFolderAutoRegisterResult autoRegisterRootFolders(@PathParam("id") UUID id) {
    return serverService.autoRegisterRootFolders(id);
  }
}
