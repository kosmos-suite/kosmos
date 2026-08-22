package de.oppahansi.kosmos.library.dto;

import java.util.UUID;

/**
 * One file {@code ImportMatchService} found under a scanned path, with its best-effort library
 * match — never authoritative, always reviewable/overridable before {@code
 * BulkImportResource#commit} actually imports anything. {@code suggestedMediaItemId} is the exact
 * id {@link de.oppahansi.kosmos.library.ImportService#importPath} expects (a movie's own id, or a
 * specific episode/anime-episode's — never the owning show/anime's), so a client can pass it back
 * unmodified to commit that suggestion, or substitute a different one from its own search/override
 * flow.
 */
public record ImportCandidate(
    String sourcePath,
    long sizeBytes,
    /**
     * The title parsed out of the filename — for display, not matching (see {@code
     * ParsedRelease#cleanTitle}/{@code ParsedAnimeRelease#showTitle} for what matching actually
     * reads).
     */
    String parsedTitle,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer absoluteEpisodeNumber,
    UUID suggestedMediaItemId,
    String suggestedMediaItemTitle,
    /**
     * {@code movie}, {@code episode}, or {@code anime_episode} — matches {@code
     * MediaItem#contentType}, null alongside a null suggestion.
     */
    String suggestedContentType,
    /**
     * True when more than one equally-plausible library title matched — {@code
     * suggestedMediaItemId} is still populated (the first match), but a human should double-check
     * it rather than trust it blindly.
     */
    boolean ambiguous) {

  public static ImportCandidate unmatched(
      String sourcePath,
      long sizeBytes,
      String parsedTitle,
      Integer season,
      Integer episode,
      Integer absolute) {
    return new ImportCandidate(
        sourcePath, sizeBytes, parsedTitle, season, episode, absolute, null, null, null, false);
  }
}
