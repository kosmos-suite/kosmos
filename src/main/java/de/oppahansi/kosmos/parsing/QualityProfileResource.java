package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.QualityProfileRequest;
import de.oppahansi.kosmos.parsing.dto.QualityProfileResponse;
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

@Path("/quality-profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QualityProfileResource {

  @Inject QualityProfileService qualityProfileService;

  @GET
  public List<QualityProfileResponse> list() {
    return qualityProfileService.listAll().stream().map(QualityProfileResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return qualityProfileService
        .findById(id)
        .map(profile -> Response.ok(QualityProfileResponse.from(profile)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(QualityProfileRequest request) {
    QualityProfileResponse response =
        QualityProfileResponse.from(qualityProfileService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") UUID id, QualityProfileRequest request) {
    return qualityProfileService
        .update(id, request)
        .map(profile -> Response.ok(QualityProfileResponse.from(profile)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") UUID id) {
    return qualityProfileService.delete(id)
        ? Response.noContent().build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }
}
