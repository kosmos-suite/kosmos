package de.oppahansi.kosmos.jellyfin;

/** A movie item read from Jellyfin's /Items — only what reconciliation actually needs. */
public record JellyfinMovie(String name, Integer year, String tmdbId, String path) {}
