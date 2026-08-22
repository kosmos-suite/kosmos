package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.SeasonPassEntry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/season-pass")
@Produces(MediaType.APPLICATION_JSON)
public class SeasonPassResource {

  @Inject SeasonPassService seasonPassService;

  @GET
  public List<SeasonPassEntry> get() {
    return seasonPassService.build();
  }
}
