package de.oppahansi.kosmos.library.naming;

import de.oppahansi.kosmos.auth.CurrentUser;
import de.oppahansi.kosmos.library.naming.dto.NamingSettingsResponse;
import de.oppahansi.kosmos.library.naming.dto.UpdateNamingSettingsRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/naming-settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NamingSettingsResource {

  @Inject NamingSettingsService namingSettingsService;
  @Inject CurrentUser currentUser;

  @GET
  public List<NamingSettingsResponse> list() {
    return namingSettingsService.listEffective().stream()
        .map(NamingSettingsResponse::from)
        .toList();
  }

  @PUT
  @Path("/{contentType}")
  public NamingSettingsResponse update(
      @PathParam("contentType") String contentType, UpdateNamingSettingsRequest request) {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Admin only");
    }
    return NamingSettingsResponse.from(namingSettingsService.update(contentType, request));
  }
}
