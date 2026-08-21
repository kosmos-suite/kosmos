package de.oppahansi.kosmos.metadata.fribb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One row of Fribb/anime-lists' cross-reference table — an AniList id alongside whatever other
 * catalogs' ids the maintainers have matched it to. Only the fields Kosmos actually uses are
 * modeled; the real file carries several more (anisearch_id, kitsu_id, livechart_id, ...).
 *
 * <p>{@code themoviedbId.tv} + {@code season.tmdb} together are what let {@code media.AnimeService}
 * pull a real per-episode tree (title, overview, air date, still) from TMDB's own {@code
 * /tv/{id}/season/{n}} endpoint via the already-built {@link
 * de.oppahansi.kosmos.metadata.tmdb.TmdbMetadataProvider#fetchSeasonEpisodes} — no separate TVDB
 * client needed, since TMDB already covers most mainstream anime as a regular TV show. {@code
 * anidbId} is also what {@code media.AnimeService} keys {@link
 * de.oppahansi.kosmos.metadata.thexem.TheXemMappingProvider}'s absolute-numbering lookup on. {@code
 * tvdbId}/{@code malId}/{@code imdbId} are recorded via {@code ExternalIdLinkService} purely as
 * cross-reference groundwork for later work (AniDB hash lookup) — nothing reads those three back
 * yet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FribbEntry(
    @JsonProperty("anilist_id") Integer anilistId,
    @JsonProperty("anidb_id") Integer anidbId,
    @JsonProperty("mal_id") Integer malId,
    @JsonProperty("tvdb_id") Integer tvdbId,
    @JsonProperty("imdb_id") List<String> imdbId,
    @JsonProperty("themoviedb_id") ThemoviedbId themoviedbId,
    Season season) {

  /**
   * {@code movie} deliberately isn't modeled — the real file has it typed inconsistently (an int
   * for most entries, a JSON array for ~1,350 others) and Kosmos never reads it anyway ({@code
   * AnimeService} only cross-references the TV side); {@code @JsonIgnoreProperties(ignoreUnknown =
   * true)} on the containing type just skips it for every shape.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ThemoviedbId(Integer tv) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Season(Integer tmdb, Integer tvdb) {}
}
