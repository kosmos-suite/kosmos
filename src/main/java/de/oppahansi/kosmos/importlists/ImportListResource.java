package de.oppahansi.kosmos.importlists;

import de.oppahansi.kosmos.importlists.dto.CreateExclusionRequest;
import de.oppahansi.kosmos.importlists.dto.CreateImportListRequest;
import de.oppahansi.kosmos.importlists.dto.ImportListExclusionResponse;
import de.oppahansi.kosmos.importlists.dto.ImportListResponse;
import de.oppahansi.kosmos.importlists.dto.UpdateImportListRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

@Path("/import-lists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportListResource {

  @Inject ImportListService importListService;

  @GET
  public List<ImportListResponse> list() {
    return importListService.listAll().stream().map(ImportListResponse::from).toList();
  }

  @POST
  public Response create(CreateImportListRequest request) {
    ImportListResponse response = ImportListResponse.from(importListService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}")
  public ImportListResponse update(@PathParam("id") UUID id, UpdateImportListRequest request) {
    return ImportListResponse.from(importListService.update(id, request));
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") UUID id) {
    importListService.delete(id);
    return Response.noContent().build();
  }

  @GET
  @Path("/exclusions")
  public List<ImportListExclusionResponse> exclusions() {
    return importListService.listExclusions().stream()
        .map(ImportListExclusionResponse::from)
        .toList();
  }

  @POST
  @Path("/exclusions")
  public Response exclude(CreateExclusionRequest request) {
    importListService.exclude(request.pluginSlug(), request.externalId(), request.title());
    return Response.status(Response.Status.CREATED).build();
  }

  @DELETE
  @Path("/exclusions/{id}")
  public Response removeExclusion(@PathParam("id") UUID id) {
    importListService.removeExclusion(id);
    return Response.noContent().build();
  }
}
