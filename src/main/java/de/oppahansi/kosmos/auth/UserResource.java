package de.oppahansi.kosmos.auth;

import de.oppahansi.kosmos.auth.dto.CreateUserRequest;
import de.oppahansi.kosmos.auth.dto.UserResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

  @Inject UserService userService;
  @Inject CurrentUser currentUser;

  @GET
  public List<UserResponse> list() {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Only an admin can list users");
    }
    return userService.listAll().stream().map(UserResponse::from).toList();
  }

  @POST
  public Response create(CreateUserRequest request) {
    UserResponse response = UserResponse.from(userService.createNative(request, currentUser.get()));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }
}
