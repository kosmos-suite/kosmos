package de.oppahansi.kosmos.scheduler;

import de.oppahansi.kosmos.scheduler.dto.ScheduledJobResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class JobResource {

  @GET
  public List<ScheduledJobResponse> list() {
    return ScheduledJob.<ScheduledJob>listAll().stream().map(ScheduledJobResponse::from).toList();
  }
}
