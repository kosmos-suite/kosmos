package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.MonitorCollectionRequest;
import de.oppahansi.kosmos.media.dto.MovieCollectionDetailResponse;
import de.oppahansi.kosmos.media.dto.MovieCollectionMemberResponse;
import de.oppahansi.kosmos.media.dto.MovieCollectionResponse;
import de.oppahansi.kosmos.metadata.ExternalIdLinkService;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.dto.TmdbCollectionResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/movie-collections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovieCollectionResource {

  @Inject MovieCollectionService movieCollectionService;
  @Inject ExternalIdLinkService externalIdLinkService;

  @GET
  public List<MovieCollectionResponse> list() {
    return movieCollectionService.listAll().stream().map(MovieCollectionResponse::from).toList();
  }

  /**
   * Live TMDB view, browsable whether or not anyone has monitored it yet — see {@code
   * MovieCollectionService}'s own doc for why a row is only ever persisted once monitoring starts.
   */
  @GET
  @Path("/tmdb/{tmdbCollectionId}")
  public Response detail(@PathParam("tmdbCollectionId") String tmdbCollectionId) {
    return movieCollectionService
        .fetchDetail(tmdbCollectionId)
        .map(detail -> Response.ok(toDetailResponse(detail)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  @Path("/tmdb/{tmdbCollectionId}/monitor")
  public MovieCollectionResponse monitor(
      @PathParam("tmdbCollectionId") String tmdbCollectionId, MonitorCollectionRequest request) {
    return MovieCollectionResponse.from(
        movieCollectionService.monitor(
            tmdbCollectionId, request != null ? request.qualityProfileId() : null));
  }

  @DELETE
  @Path("/{id}/monitor")
  public Response unmonitor(@PathParam("id") UUID id) {
    movieCollectionService.unmonitor(id);
    return Response.noContent().build();
  }

  private MovieCollectionDetailResponse toDetailResponse(TmdbCollectionResult detail) {
    var existing = movieCollectionService.findByTmdbId(detail.externalId());
    List<MovieCollectionMemberResponse> members =
        detail.members().stream().map(this::toMemberResponse).toList();
    return new MovieCollectionDetailResponse(
        detail.externalId(),
        detail.name(),
        detail.posterPath(),
        detail.backdropPath(),
        existing.map(c -> c.monitored).orElse(false),
        existing.map(c -> c.id).orElse(null),
        members);
  }

  private MovieCollectionMemberResponse toMemberResponse(MetadataSearchResult member) {
    var linked = externalIdLinkService.findLinkedMediaItem("tmdb", member.externalId());
    return new MovieCollectionMemberResponse(
        member.externalId(),
        member.title(),
        member.year(),
        member.posterPath(),
        linked.isPresent(),
        linked.map(m -> m.id).orElse(null));
  }
}
