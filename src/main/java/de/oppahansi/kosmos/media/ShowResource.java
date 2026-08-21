package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.auth.CurrentUser;
import de.oppahansi.kosmos.media.dto.CreateShowRequest;
import de.oppahansi.kosmos.media.dto.SeasonResponse;
import de.oppahansi.kosmos.media.dto.ShowDetailResponse;
import de.oppahansi.kosmos.media.dto.ShowResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/shows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShowResource {

  @Inject ShowService showService;
  @Inject CurrentUser currentUser;

  @GET
  public List<ShowResponse> list() {
    return showService.listAll().stream().map(ShowResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return showService
        .findById(id)
        .map(show -> Response.ok(toDetail(show)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  /**
   * Direct add is admin-only — non-admins go through {@code RequestResource} for review instead.
   */
  @POST
  public Response create(CreateShowRequest request) {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Admin only");
    }
    ShowResponse response = ShowResponse.from(showService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}/quality-profile")
  public Response updateQualityProfile(
      @PathParam("id") UUID id, UpdateMovieQualityProfileRequest request) {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Admin only");
    }
    return showService
        .updateQualityProfile(id, request.qualityProfileId())
        .map(show -> Response.ok(toDetail(show)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  private ShowDetailResponse toDetail(Show show) {
    List<Season> seasons = showService.seasonsFor(show.mediaItemId);
    Map<UUID, List<Episode>> episodesBySeason = new HashMap<>();
    List<UUID> allEpisodeIds = new ArrayList<>();
    for (Season season : seasons) {
      List<Episode> episodes = showService.episodesFor(season.id);
      episodesBySeason.put(season.id, episodes);
      episodes.forEach(e -> allEpisodeIds.add(e.mediaItemId));
    }
    Map<UUID, String> statusByEpisode = MediaItemStatus.forMediaItems(allEpisodeIds);

    List<SeasonResponse> seasonResponses =
        seasons.stream()
            .map(
                season ->
                    SeasonResponse.from(season, episodesBySeason.get(season.id), statusByEpisode))
            .toList();
    return ShowDetailResponse.from(show, seasonResponses);
  }
}
