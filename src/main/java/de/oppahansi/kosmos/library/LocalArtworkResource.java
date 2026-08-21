package de.oppahansi.kosmos.library;

import de.oppahansi.kosmos.media.MediaItem;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * Serves the local-poster/local-backdrop fallback an {@code <img>} tries after TMDB has no artwork
 * — see {@link LocalArtworkService}.
 */
@Path("/media-items/{id}")
public class LocalArtworkResource {

  @Inject LocalArtworkService localArtworkService;

  @GET
  @Path("/local-poster")
  public Response getPoster(@PathParam("id") UUID id) {
    return serve(id, LocalArtworkService.Kind.POSTER);
  }

  @GET
  @Path("/local-backdrop")
  public Response getBackdrop(@PathParam("id") UUID id) {
    return serve(id, LocalArtworkService.Kind.BACKDROP);
  }

  private Response serve(UUID id, LocalArtworkService.Kind kind) {
    MediaItem mediaItem =
        MediaItem.<MediaItem>findByIdOptional(id)
            .orElseThrow(() -> new NotFoundException("Unknown media item: " + id));

    return localArtworkService
        .find(mediaItem, kind)
        .map(
            artwork -> {
              CacheControl cacheControl = new CacheControl();
              cacheControl.setMaxAge(86400);
              return Response.ok(artwork.bytes(), artwork.contentType())
                  .cacheControl(cacheControl)
                  .build();
            })
        .orElseThrow(() -> new NotFoundException("No local artwork for " + id));
  }
}
