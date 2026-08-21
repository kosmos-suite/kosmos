package de.oppahansi.kosmos.notifications;

import de.oppahansi.kosmos.notifications.dto.CreateNotifierRequest;
import de.oppahansi.kosmos.notifications.dto.NotifierResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/notifiers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotifierResource {

  @Inject NotifierService notifierService;

  @GET
  public List<NotifierResponse> list() {
    return notifierService.listAll().stream().map(NotifierResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return notifierService
        .findById(id)
        .map(notifier -> Response.ok(NotifierResponse.from(notifier)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(CreateNotifierRequest request) {
    NotifierResponse response = NotifierResponse.from(notifierService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }
}
