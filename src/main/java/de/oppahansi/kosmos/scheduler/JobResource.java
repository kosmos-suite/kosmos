package de.oppahansi.kosmos.scheduler;

import de.oppahansi.kosmos.scheduler.dto.JobRunResponse;
import de.oppahansi.kosmos.scheduler.dto.ScheduledJobResponse;
import de.oppahansi.kosmos.scheduler.dto.UpdateScheduledJobRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobResource {

  private static final int DEFAULT_RUN_HISTORY = 20;

  @Inject JobService jobService;

  @GET
  public List<ScheduledJobResponse> list() {
    return jobService.listAll().stream().map(ScheduledJobResponse::from).toList();
  }

  /** Most recent runs first — the history a job's expanded row on the settings page reads. */
  @GET
  @Path("/{name}/runs")
  public List<JobRunResponse> runs(
      @PathParam("name") String name, @QueryParam("limit") Integer limit) {
    return jobService.runsFor(name, limit == null ? DEFAULT_RUN_HISTORY : limit).stream()
        .map(JobRunResponse::from)
        .toList();
  }

  /**
   * Triggers the job immediately, same as Seerr's "Run Now" — bypasses the interval but not
   * enabled/already-running. Runs synchronously (matches {@code POST /jellyfin-servers/{id}/sync}'s
   * existing convention for a user-triggered background operation) — every current job handler
   * finishes in well under a request timeout.
   */
  @POST
  @Path("/{name}/run")
  public Response runNow(@PathParam("name") String name) {
    if (!jobService.handlerExists(name)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return jobService
        .runNow(name)
        .map(run -> Response.ok(JobRunResponse.from(run)).build())
        .orElse(Response.status(Response.Status.CONFLICT).build());
  }

  @PUT
  @Path("/{name}")
  public Response update(@PathParam("name") String name, UpdateScheduledJobRequest request) {
    if (request.intervalSeconds() < 10) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("intervalSeconds must be at least 10")
          .build();
    }
    return jobService
        .update(name, request.enabled(), request.intervalSeconds())
        .map(job -> Response.ok(ScheduledJobResponse.from(job)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }
}
