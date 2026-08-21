package de.oppahansi.kosmos.metadata.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A not-yet-owned title's detail page — same shape as {@link MediaDetailExtras} plus the base
 * TMDB/AniList fields (title, poster, overview, ...) a real {@code Movie}/{@code Show}/{@code
 * Anime} row would otherwise carry, since none exists yet. Backs {@code GET
 * /movies|shows/tmdb/{externalId}} and {@code GET /anime/anilist/{externalId}} — the "Add to
 * Library"/"Request" screen a card for a title Kosmos doesn't have yet links to, instead of falling
 * back to a search.
 *
 * <p>{@code seasons} (shows) and {@code episodes} (anime, flat — no season grouping) mirror the
 * same season/episode tree a real {@code Show}/{@code Anime} would carry, so the preview screen can
 * render the identical Seasons/Episodes section an owned title's detail page does. Movies leave
 * both empty. Every episode here is implicitly "missing" — nothing's been downloaded for a title
 * that isn't in the library yet — so neither preview record carries a status field.
 */
public record MediaPreview(
    String externalId,
    String pluginSlug,
    String mediaType,
    String title,
    Integer year,
    String overview,
    String posterPath,
    String backdropPath,
    List<String> genres,
    List<MediaDetailExtras.Fact> facts,
    Double voteAverage,
    Integer voteCount,
    String certification,
    List<MediaDetailExtras.CastMember> cast,
    List<MetadataSearchItem> similar,
    List<PreviewSeason> seasons,
    List<PreviewEpisode> episodes) {

  public record PreviewSeason(
      int seasonNumber, String name, Integer episodeCount, List<PreviewEpisode> episodes) {}

  public record PreviewEpisode(int episodeNumber, String title, LocalDate airDate) {}
}
