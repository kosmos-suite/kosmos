package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.downloads.dto.CreateDownloadClientRequest;
import de.oppahansi.kosmos.downloads.dto.DownloadClientResponse;
import de.oppahansi.kosmos.downloads.dto.TestDownloadClientRequest;
import de.oppahansi.kosmos.downloads.dto.TestDownloadClientResult;
import de.oppahansi.kosmos.downloads.dto.UpdatePathMappingRequest;
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

@Path("/download-clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DownloadClientResource {

  @Inject DownloadClientService downloadClientService;

  @GET
  public List<DownloadClientResponse> list() {
    return downloadClientService.listAll().stream().map(DownloadClientResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return downloadClientService
        .findById(id)
        .map(client -> Response.ok(DownloadClientResponse.from(client)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(CreateDownloadClientRequest request) {
    DownloadClientResponse response =
        DownloadClientResponse.from(downloadClientService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}/path-mapping")
  public DownloadClientResponse updatePathMapping(
      @PathParam("id") UUID id, UpdatePathMappingRequest request) {
    return DownloadClientResponse.from(downloadClientService.updatePathMapping(id, request));
  }

  @POST
  @Path("/test")
  public TestDownloadClientResult test(TestDownloadClientRequest request) {
    return downloadClientService.testConnection(request);
  }
}
