package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.CustomFormatRequest;
import de.oppahansi.kosmos.parsing.dto.CustomFormatResponse;
import de.oppahansi.kosmos.parsing.trash.TrashGuidesImportService;
import de.oppahansi.kosmos.parsing.trash.TrashImportResult;
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

@Path("/custom-formats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomFormatResource {

  @Inject CustomFormatService customFormatService;
  @Inject TrashGuidesImportService trashGuidesImportService;

  @GET
  public List<CustomFormatResponse> list() {
    return customFormatService.listAll().stream().map(CustomFormatResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return customFormatService
        .findById(id)
        .map(format -> Response.ok(CustomFormatResponse.from(format)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(CustomFormatRequest request) {
    CustomFormatResponse response = CustomFormatResponse.from(customFormatService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") UUID id, CustomFormatRequest request) {
    return customFormatService
        .update(id, request)
        .map(format -> Response.ok(CustomFormatResponse.from(format)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") UUID id) {
    return customFormatService.delete(id)
        ? Response.noContent().build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  @POST
  @Path("/import/trash-guides")
  public TrashImportResult importFromTrashGuides() {
    return trashGuidesImportService.importAll();
  }
}
