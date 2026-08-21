package de.oppahansi.kosmos.auth.dto;

/**
 * Payload for a native (non-Jellyfin) account. role is ignored for the very first user, who is
 * always ADMIN.
 */
public record CreateUserRequest(
    String username, String displayName, String password, String role) {}
