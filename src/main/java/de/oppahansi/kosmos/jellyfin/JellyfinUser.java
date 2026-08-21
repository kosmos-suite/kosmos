package de.oppahansi.kosmos.jellyfin;

/** A user account read from Jellyfin's own /Users list. */
public record JellyfinUser(String id, String name, boolean isAdmin) {}
