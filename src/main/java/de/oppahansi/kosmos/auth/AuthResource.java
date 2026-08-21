package de.oppahansi.kosmos.auth;

import de.oppahansi.kosmos.auth.dto.LoginRequest;
import de.oppahansi.kosmos.auth.dto.UserResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

  @Inject AuthService authService;
  @Inject CurrentUser currentUser;

  @POST
  @Path("/login")
  public Response login(LoginRequest request) {
    return authService
        .login(request.username(), request.password())
        .map(
            session ->
                Response.ok(UserResponse.from(session.user))
                    .cookie(
                        sessionCookie(
                            session.token,
                            (int) Duration.between(Instant.now(), session.expiresAt).toSeconds()))
                    .build())
        .orElse(Response.status(Response.Status.UNAUTHORIZED).build());
  }

  @POST
  @Path("/logout")
  public Response logout(@CookieParam(SessionFilter.COOKIE_NAME) String token) {
    if (token != null) {
      authService.logout(token);
    }
    return Response.ok().cookie(sessionCookie("", 0)).build();
  }

  @GET
  @Path("/me")
  public Response me() {
    return currentUser
        .get()
        .map(user -> Response.ok(UserResponse.from(user)).build())
        .orElse(Response.status(Response.Status.UNAUTHORIZED).build());
  }

  private NewCookie sessionCookie(String value, int maxAgeSeconds) {
    return new NewCookie.Builder(SessionFilter.COOKIE_NAME)
        .value(value)
        .path("/")
        .httpOnly(true)
        .maxAge(maxAgeSeconds)
        .build();
  }
}
