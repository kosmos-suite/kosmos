package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.QualityDefinitionRequest;
import de.oppahansi.kosmos.parsing.dto.QualityDefinitionResponse;
import de.oppahansi.kosmos.parsing.trash.QualityDefinitionImportService;
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

@Path("/quality-definitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QualityDefinitionResource {

  @Inject QualityDefinitionService qualityDefinitionService;
  @Inject QualityDefinitionImportService qualityDefinitionImportService;

  @GET
  public List<QualityDefinitionResponse> list() {
    return qualityDefinitionService.listAll().stream()
        .map(QualityDefinitionResponse::from)
        .toList();
  }

  @POST
  public Response create(QualityDefinitionRequest request) {
    QualityDefinitionResponse response =
        QualityDefinitionResponse.from(qualityDefinitionService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") UUID id, QualityDefinitionRequest request) {
    return qualityDefinitionService
        .update(id, request)
        .map(definition -> Response.ok(QualityDefinitionResponse.from(definition)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") UUID id) {
    return qualityDefinitionService.delete(id)
        ? Response.noContent().build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  @POST
  @Path("/import/trash-guides")
  public TrashImportResult importFromTrashGuides() {
    return qualityDefinitionImportService.importAll();
  }
}
