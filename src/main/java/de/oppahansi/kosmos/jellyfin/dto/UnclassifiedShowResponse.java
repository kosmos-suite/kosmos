package de.oppahansi.kosmos.jellyfin.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.jellyfin.UnclassifiedShow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Backs the "Needs Review" collection — {@code posterPath}/{@code overview} are a best-effort TMDB
 * lookup by {@code tmdbId} (the one identity Kosmos always has for a pending item), not from the
 * Anime side, since that's exactly the piece that failed to resolve. {@code seasons} is the
 * season/episode breakdown Jellyfin's own scanned files reported at flag time (see the entity's
 * {@code seasonsJson}) — empty when Jellyfin had no episode files for this item yet.
 */
public record UnclassifiedShowResponse(
    UUID id,
    String name,
    Integer year,
    String tmdbId,
    String anilistId,
    String posterPath,
    String overview,
    String reason,
    List<SeasonEpisodeCount> seasons,
    Instant detectedAt) {

  private static final TypeReference<List<SeasonEpisodeCount>> SEASONS_TYPE =
      new TypeReference<>() {};

  public static UnclassifiedShowResponse from(
      UnclassifiedShow pending, String posterPath, String overview, ObjectMapper objectMapper) {
    return new UnclassifiedShowResponse(
        pending.id,
        pending.name,
        pending.year,
        pending.tmdbId,
        pending.anilistId,
        posterPath,
        overview,
        pending.reason,
        parseSeasons(pending.seasonsJson, objectMapper),
        pending.detectedAt);
  }

  private static List<SeasonEpisodeCount> parseSeasons(String json, ObjectMapper objectMapper) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, SEASONS_TYPE);
    } catch (Exception e) {
      return List.of();
    }
  }
}
