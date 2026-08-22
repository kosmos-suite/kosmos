package de.oppahansi.kosmos.jellyfin.dto;

/**
 * Outcome of one library sync run. linked = existing Kosmos movie/show, newly-recorded file.
 * created = brand-new movie/show. Show fields mirror the movie ones one level up (per-series, not
 * per-file — a series is "linked" if any new episode file was matched to it). episodeFilesLinked is
 * the total episode file count across every show, since per-show file counts wouldn't fit this flat
 * a result.
 */
public record JellyfinLibrarySyncResult(
    int scanned,
    int linked,
    int created,
    int skippedNoTmdbId,
    int alreadySynced,
    int showsScanned,
    int showsLinked,
    int showsCreated,
    int showsSkippedNoTmdbId,
    int showsAlreadySynced,
    int episodeFilesLinked) {}
