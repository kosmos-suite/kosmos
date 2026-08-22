package de.oppahansi.kosmos.jellyfin;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.jellyfin.dto.ResolveAsAnimeRequest;
import de.oppahansi.kosmos.jellyfin.dto.UnclassifiedShowResponse;
import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
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
import java.util.Optional;
import java.util.UUID;

/**
 * The "Needs Review" queue — Jellyfin "tvshows" items {@code JellyfinSyncService} found an anime
 * signal for but couldn't confirm from AniList, so neither a {@code Show} nor an {@code Anime} row
 * was created for them. A human resolves each one from here instead.
 */
@Path("/unclassified-shows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UnclassifiedShowResource {

  @Inject JellyfinSyncService syncService;
  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject ObjectMapper objectMapper;

  @GET
  public List<UnclassifiedShowResponse> list() {
    return UnclassifiedShow.<UnclassifiedShow>listAll().stream()
        .map(
            pending -> {
              Optional<MetadataSearchResult> tmdb =
                  tmdbMetadataProvider.fetchShowById(pending.tmdbId);
              return UnclassifiedShowResponse.from(
                  pending,
                  tmdb.map(MetadataSearchResult::posterPath).orElse(null),
                  tmdb.map(MetadataSearchResult::overview).orElse(null),
                  objectMapper);
            })
        .toList();
  }

  @POST
  @Path("/{id}/resolve-as-show")
  public Response resolveAsShow(@PathParam("id") UUID id) {
    syncService.resolveAsShow(id);
    return Response.noContent().build();
  }

  @POST
  @Path("/{id}/resolve-as-anime")
  public Response resolveAsAnime(@PathParam("id") UUID id, ResolveAsAnimeRequest request) {
    syncService.resolveAsAnime(id, request.anilistId());
    return Response.noContent().build();
  }

  @DELETE
  @Path("/{id}")
  public Response dismiss(@PathParam("id") UUID id) {
    syncService.dismissUnclassified(id);
    return Response.noContent().build();
  }
}
