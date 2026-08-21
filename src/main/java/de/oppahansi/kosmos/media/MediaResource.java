package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.auth.CurrentUser;
import de.oppahansi.kosmos.library.LibraryFile;
import de.oppahansi.kosmos.library.dto.LibraryFileResponse;
import de.oppahansi.kosmos.media.dto.CreateMovieRequest;
import de.oppahansi.kosmos.media.dto.MovieResponse;
import de.oppahansi.kosmos.media.dto.UpdateMovieQualityProfileRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MediaResource {

  @Inject MovieService movieService;
  @Inject CurrentUser currentUser;

  @GET
  public List<MovieResponse> list() {
    return movieService.listAll().stream().map(MovieResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return movieService
        .findById(id)
        .map(movie -> Response.ok(MovieResponse.from(movie)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  /**
   * Direct add is admin-only — non-admins go through {@code RequestResource} for review instead.
   */
  @POST
  public Response create(CreateMovieRequest request) {
    requireAdmin();
    MovieResponse response = MovieResponse.from(movieService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  /** Backs {@code MovieDetailPage}'s file/probe section — see {@code LibraryFile}. */
  @GET
  @Path("/{id}/library-files")
  public List<LibraryFileResponse> libraryFiles(@PathParam("id") UUID id) {
    return LibraryFile.<LibraryFile>list("mediaItem.id", id).stream()
        .map(LibraryFileResponse::from)
        .toList();
  }

  @PUT
  @Path("/{id}/quality-profile")
  public Response updateQualityProfile(
      @PathParam("id") UUID id, UpdateMovieQualityProfileRequest request) {
    requireAdmin();
    return movieService
        .updateQualityProfile(id, request.qualityProfileId())
        .map(movie -> Response.ok(MovieResponse.from(movie)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  private void requireAdmin() {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Admin only");
    }
  }
}
