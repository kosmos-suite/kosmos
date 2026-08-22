package de.oppahansi.kosmos.jellyfin.dto;

/**
 * Outcome of one user import run. updated = an already-linked account's role/display name actually
 * changed (e.g. Jellyfin granted/revoked admin). skippedNotSelected = accounts present on the
 * server but excluded by {@link de.oppahansi.kosmos.jellyfin.JellyfinServer#selectedUserIds}.
 */
public record JellyfinUserSyncResult(
    int scanned, int created, int updated, int skippedNotSelected) {}
