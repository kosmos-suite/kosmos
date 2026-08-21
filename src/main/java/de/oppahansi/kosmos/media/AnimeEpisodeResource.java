package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.downloads.Grab;
import de.oppahansi.kosmos.library.LibraryFile;
import de.oppahansi.kosmos.media.dto.AnimeEpisodeDetailResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * Backs the interactive-search/manual-grab pages when their target is an anime episode — mirrors
 * {@link EpisodeResource}. Grabbing itself doesn't need an anime-specific resource: {@link
 * de.oppahansi.kosmos.downloads.EpisodeGrabResource} already operates on any {@code MediaItem} id,
 * anime episode or TV episode alike.
 */
@Path("/anime-episodes")
@Produces(MediaType.APPLICATION_JSON)
public class AnimeEpisodeResource {

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return AnimeEpisode.<AnimeEpisode>findByIdOptional(id)
        .map(
            episode -> Response.ok(AnimeEpisodeDetailResponse.from(episode, statusFor(id))).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  private String statusFor(UUID episodeMediaItemId) {
    if (LibraryFile.count("mediaItem.id", episodeMediaItemId) > 0) {
      return "AVAILABLE";
    }
    Grab grab =
        Grab.<Grab>find("release.mediaItem.id = ?1 order by grabbedAt desc", episodeMediaItemId)
            .firstResult();
    return grab != null ? grab.status : "MISSING";
  }
}
