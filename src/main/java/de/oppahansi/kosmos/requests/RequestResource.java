package de.oppahansi.kosmos.requests;

import de.oppahansi.kosmos.auth.CurrentUser;
import de.oppahansi.kosmos.auth.User;
import de.oppahansi.kosmos.requests.dto.CreateRequestRequest;
import de.oppahansi.kosmos.requests.dto.DecideRequestRequest;
import de.oppahansi.kosmos.requests.dto.RequestResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {

  @Inject RequestService requestService;
  @Inject CurrentUser currentUser;

  @GET
  public List<RequestResponse> list() {
    User user = requireUser();
    return requestService.listFor(user).stream()
        .map(r -> RequestResponse.from(r, user.id))
        .toList();
  }

  @POST
  public Response create(CreateRequestRequest body) {
    User user = requireUser();
    Request request = requestService.create(user, body);
    return Response.status(Response.Status.CREATED)
        .entity(RequestResponse.from(request, user.id))
        .build();
  }

  @POST
  @Path("/{id}/approve")
  public RequestResponse approve(@PathParam("id") UUID id, DecideRequestRequest body) {
    User admin = requireAdmin();
    Request request =
        requestService.approve(id, admin, body != null ? body.qualityProfileId() : null);
    return RequestResponse.from(request, admin.id);
  }

  @POST
  @Path("/{id}/decline")
  public RequestResponse decline(@PathParam("id") UUID id, DecideRequestRequest body) {
    User admin = requireAdmin();
    Request request = requestService.decline(id, admin, body != null ? body.note() : null);
    return RequestResponse.from(request, admin.id);
  }

  private User requireUser() {
    return currentUser
        .get()
        .orElseThrow(
            () -> new WebApplicationException("Login required", Response.Status.UNAUTHORIZED));
  }

  private User requireAdmin() {
    User user = requireUser();
    if (!user.isAdmin()) {
      throw new ForbiddenException("Only an admin can decide requests");
    }
    return user;
  }
}
