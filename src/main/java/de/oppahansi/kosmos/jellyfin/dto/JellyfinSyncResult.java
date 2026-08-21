package de.oppahansi.kosmos.jellyfin.dto;

/**
 * Outcome of one sync run. linked = existing Kosmos movie, newly-recorded file. created = brand-new
 * movie. Show fields mirror the movie ones one level up (per-series, not per-file — a series is
 * "linked" if any new episode file was matched to it). episodeFilesLinked is the total episode file
 * count across every show, since per-show file counts wouldn't fit this flat a result. usersUpdated
 * = an already-linked account's role/display name changed (e.g. Jellyfin granted/revoked admin).
 */
public record JellyfinSyncResult(
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
    int episodeFilesLinked,
    int usersCreated,
    int usersUpdated) {}
