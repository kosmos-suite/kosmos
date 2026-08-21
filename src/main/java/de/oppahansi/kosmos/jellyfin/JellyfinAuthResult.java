package de.oppahansi.kosmos.jellyfin;

/** Result of a successful AuthenticateByName call. */
public record JellyfinAuthResult(String userId, String name, boolean isAdmin) {}
