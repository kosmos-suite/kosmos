package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.auth.CurrentUser;
import de.oppahansi.kosmos.media.dto.AnimeDetailResponse;
import de.oppahansi.kosmos.media.dto.AnimeEpisodeResponse;
import de.oppahansi.kosmos.media.dto.AnimeResponse;
import de.oppahansi.kosmos.media.dto.CreateAnimeRequest;
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
import java.util.Map;
import java.util.UUID;

@Path("/anime")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnimeResource {

  @Inject AnimeService animeService;
  @Inject CurrentUser currentUser;

  @GET
  public List<AnimeResponse> list() {
    return animeService.listAll().stream().map(AnimeResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return animeService
        .findById(id)
        .map(anime -> Response.ok(toDetail(anime)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  /**
   * Direct add is admin-only — non-admins go through {@code RequestResource} for review instead.
   */
  @POST
  public Response create(CreateAnimeRequest request) {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Admin only");
    }
    AnimeResponse response = AnimeResponse.from(animeService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}/quality-profile")
  public Response updateQualityProfile(
      @PathParam("id") UUID id, UpdateMovieQualityProfileRequest request) {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Admin only");
    }
    return animeService
        .updateQualityProfile(id, request.qualityProfileId())
        .map(anime -> Response.ok(toDetail(anime)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  /** Backs {@code AnimeDetailPage}'s Details/More Like This sections. */
  @GET
  @Path("/{id}/detail-extras")
  public Response detailExtras(@PathParam("id") UUID id) {
    return animeService
        .detailExtras(id)
        .map(extras -> Response.ok(extras).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  private AnimeDetailResponse toDetail(Anime anime) {
    List<AnimeEpisode> episodes = animeService.episodesFor(anime.mediaItemId);
    Map<UUID, String> statusByEpisode =
        MediaItemStatus.forMediaItems(episodes.stream().map(e -> e.mediaItemId).toList());
    List<AnimeEpisodeResponse> episodeResponses =
        episodes.stream()
            .map(
                e ->
                    AnimeEpisodeResponse.from(
                        e, statusByEpisode.getOrDefault(e.mediaItemId, "MISSING")))
            .toList();
    return AnimeDetailResponse.from(anime, episodeResponses);
  }
}
