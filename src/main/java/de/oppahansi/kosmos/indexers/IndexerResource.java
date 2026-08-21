package de.oppahansi.kosmos.indexers;

import de.oppahansi.kosmos.indexers.dto.CreateIndexerRequest;
import de.oppahansi.kosmos.indexers.dto.IndexerResponse;
import de.oppahansi.kosmos.indexers.dto.ScoredSearchResult;
import de.oppahansi.kosmos.indexers.dto.TorznabResult;
import de.oppahansi.kosmos.parsing.QualityDefinitionService;
import de.oppahansi.kosmos.parsing.QualityProfile;
import de.oppahansi.kosmos.parsing.QualityProfileService;
import de.oppahansi.kosmos.parsing.ReleaseParser;
import de.oppahansi.kosmos.parsing.ScoringEngine;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import de.oppahansi.kosmos.parsing.dto.ScoredRelease;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Path("/indexers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IndexerResource {

  @Inject IndexerService indexerService;
  @Inject TorznabClient torznabClient;
  @Inject ReleaseParser releaseParser;
  @Inject ScoringEngine scoringEngine;
  @Inject QualityProfileService qualityProfileService;
  @Inject QualityDefinitionService qualityDefinitionService;

  @GET
  public List<IndexerResponse> list() {
    return indexerService.listAll().stream().map(IndexerResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return indexerService
        .findById(id)
        .map(indexer -> Response.ok(IndexerResponse.from(indexer)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  public Response create(CreateIndexerRequest request) {
    IndexerResponse response = IndexerResponse.from(indexerService.create(request));
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @GET
  @Path("/{id}/search")
  public Response search(
      @PathParam("id") UUID id,
      @QueryParam("q") String query,
      @QueryParam("qualityProfileId") UUID qualityProfileId,
      @QueryParam("runtimeMinutes") Integer runtimeMinutes) {
    return indexerService
        .findById(id)
        .map(indexer -> searchIndexer(indexer, query, qualityProfileId, runtimeMinutes))
        .orElse(Response.status(Response.Status.NOT_FOUND).build());
  }

  private Response searchIndexer(
      Indexer indexer, String query, UUID qualityProfileId, Integer runtimeMinutes) {
    QualityProfile profile =
        qualityProfileId == null
            ? null
            : qualityProfileService
                .findById(qualityProfileId)
                .orElseThrow(
                    () ->
                        new BadRequestException("Unknown quality profile id: " + qualityProfileId));

    List<TorznabResult> rawResults;
    try {
      rawResults = torznabClient.search(indexer.baseUrl, indexer.apiKey, query);
    } catch (IOException | InterruptedException e) {
      return Response.status(Response.Status.BAD_GATEWAY).entity(e.getMessage()).build();
    }

    List<ScoredSearchResult> results =
        rawResults.stream().map(raw -> scoreOne(raw, profile, runtimeMinutes)).toList();
    if (profile != null) {
      results =
          results.stream()
              .sorted(Comparator.comparingInt(ScoredSearchResult::score).reversed())
              .toList();
    }
    return Response.ok(results).build();
  }

  private ScoredSearchResult scoreOne(
      TorznabResult raw, QualityProfile profile, Integer runtimeMinutes) {
    ParsedRelease parsed = releaseParser.parse(raw.title());
    if (profile == null) {
      return ScoredSearchResult.unscored(raw, parsed);
    }
    ScoredRelease scored = scoringEngine.score(parsed, profile);
    String sizeGateReason =
        qualityDefinitionService.checkSizeGate(parsed, raw.sizeBytes(), runtimeMinutes);
    boolean passesCutoff = scored.passesCutoff() && sizeGateReason == null;
    return new ScoredSearchResult(
        raw,
        parsed,
        scored.totalScore(),
        scored.cutoffScore(),
        passesCutoff,
        scored.formatBreakdown(),
        sizeGateReason);
  }
}
