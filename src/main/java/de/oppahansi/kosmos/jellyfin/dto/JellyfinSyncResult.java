package de.oppahansi.kosmos.jellyfin.dto;

/**
 * Outcome of one sync run. linked = existing Kosmos movie, newly-recorded file. created = brand-new
 * movie. usersUpdated = an already-linked account's role/display name changed (e.g. Jellyfin
 * granted/revoked admin).
 */
public record JellyfinSyncResult(
    int scanned,
    int linked,
    int created,
    int skippedNoTmdbId,
    int alreadySynced,
    int usersCreated,
    int usersUpdated) {}
