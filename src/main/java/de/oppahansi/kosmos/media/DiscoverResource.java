package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.DiscoverItem;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** Backs Discover/Home's real rows — see {@link DiscoverService}. */
@Path("/discover")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoverResource {

  @Inject DiscoverService discoverService;

  @GET
  @Path("/recent")
  public List<DiscoverItem> recent() {
    return discoverService.recentlyAdded();
  }

  @GET
  @Path("/trending")
  public List<DiscoverItem> trending() {
    return discoverService.trending();
  }

  @GET
  @Path("/popular")
  public List<DiscoverItem> popular() {
    return discoverService.popular();
  }
}
