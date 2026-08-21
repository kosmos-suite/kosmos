package de.oppahansi.kosmos.downloads;

import de.oppahansi.kosmos.downloads.dto.GrabRequest;
import de.oppahansi.kosmos.downloads.dto.GrabResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * Grabbing an episode is identical to grabbing a movie as far as {@link GrabService} is concerned —
 * both are just a {@code MediaItem} id — so this delegates to the exact same service {@link
 * GrabResource} uses rather than duplicating any logic.
 */
@Path("/episodes/{episodeId}/grab")
@Produces(MediaType.APPLICATION_JSON)
public class EpisodeGrabResource {

  @Inject GrabService grabService;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response grab(@PathParam("episodeId") UUID episodeId, GrabRequest request) {
    return grabService
        .grab(episodeId, request)
        .map(
            grab ->
                Response.status(Response.Status.CREATED).entity(GrabResponse.from(grab)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  @Path("/file")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response grabFile(
      @PathParam("episodeId") UUID episodeId,
      @RestForm("file") FileUpload file,
      @RestForm String title,
      @RestForm UUID downloadClientId) {
    byte[] content;
    try {
      content = Files.readAllBytes(file.filePath());
    } catch (IOException e) {
      throw new WebApplicationException(
          "Failed to read uploaded file", Response.Status.BAD_REQUEST);
    }
    String filename = file.fileName() != null ? file.fileName() : "upload.torrent";
    String releaseTitle = title == null || title.isBlank() ? filename : title;

    return grabService
        .grabFile(episodeId, downloadClientId, releaseTitle, content, filename)
        .map(
            grab ->
                Response.status(Response.Status.CREATED).entity(GrabResponse.from(grab)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }
}
