package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.DiscoverItem;
import de.oppahansi.kosmos.media.dto.GenreTile;
import de.oppahansi.kosmos.media.dto.StudioTile;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
  public List<DiscoverItem> trending(
      @QueryParam("window") @DefaultValue("week") String window,
      @QueryParam("mediaType") @DefaultValue("all") String mediaType,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.trending(window, mediaType, page, excludeLanguages);
  }

  @GET
  @Path("/popular")
  public List<DiscoverItem> popular(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.popular(page, excludeLanguages);
  }

  @GET
  @Path("/because-you-added")
  public Response becauseYouAdded() {
    return discoverService
        .becauseYouAdded()
        .map(result -> Response.ok(result).build())
        .orElseGet(() -> Response.noContent().build());
  }

  @GET
  @Path("/upcoming-movies")
  public List<DiscoverItem> upcomingMovies(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.upcomingMovies(page, excludeLanguages);
  }

  @GET
  @Path("/popular-tv")
  public List<DiscoverItem> popularTv(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.popularTv(page, excludeLanguages);
  }

  @GET
  @Path("/upcoming-tv")
  public List<DiscoverItem> upcomingTv(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.upcomingTv(page, excludeLanguages);
  }

  @GET
  @Path("/genres/movie")
  public List<GenreTile> movieGenres() {
    return discoverService.movieGenres();
  }

  @GET
  @Path("/genres/tv")
  public List<GenreTile> tvGenres() {
    return discoverService.tvGenres();
  }

  @GET
  @Path("/genre/movie/{id}")
  public List<DiscoverItem> moviesByGenre(
      @PathParam("id") int id,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.moviesByGenre(id, page, excludeLanguages);
  }

  @GET
  @Path("/genre/tv/{id}")
  public List<DiscoverItem> tvByGenre(
      @PathParam("id") int id,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.tvByGenre(id, page, excludeLanguages);
  }

  @GET
  @Path("/studios")
  public List<StudioTile> studios() {
    return discoverService.studios();
  }

  @GET
  @Path("/networks")
  public List<StudioTile> networks() {
    return discoverService.networks();
  }

  @GET
  @Path("/studio/{id}")
  public List<DiscoverItem> moviesByStudio(
      @PathParam("id") int id,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.moviesByStudio(id, page, excludeLanguages);
  }

  @GET
  @Path("/network/{id}")
  public List<DiscoverItem> tvByNetwork(
      @PathParam("id") int id,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("excludeLanguages") @DefaultValue("") String excludeLanguages) {
    return discoverService.tvByNetwork(id, page, excludeLanguages);
  }
}
