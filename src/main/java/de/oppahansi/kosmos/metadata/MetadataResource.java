package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.metadata.dto.MetadataSearchItem;
import de.oppahansi.kosmos.metadata.dto.MetadataStatusResponse;
import de.oppahansi.kosmos.metadata.dto.TmdbTestResult;
import de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/metadata")
@Produces(MediaType.APPLICATION_JSON)
public class MetadataResource {

  @Inject TmdbMetadataProvider tmdbMetadataProvider;
  @Inject MetadataSearchService metadataSearchService;

  /**
   * Movie, TV, and anime results merged — the frontend's kind filter (All/Movies/Series/Anime) is
   * client-side. See {@link MetadataSearchService} for the library cross-reference.
   */
  @GET
  @Path("/search")
  public List<MetadataSearchItem> search(@QueryParam("q") String query) {
    return metadataSearchService.search(query);
  }

  /** Backs the onboarding wizard's metadata-source step. */
  @GET
  @Path("/status")
  public MetadataStatusResponse status() {
    return new MetadataStatusResponse(tmdbMetadataProvider.isConfigured());
  }

  /** Real check against TMDB itself — {@link #status} only reports whether a key is set. */
  @POST
  @Path("/tmdb/test")
  public TmdbTestResult testTmdb() {
    return new TmdbTestResult(tmdbMetadataProvider.testConnection());
  }
}
