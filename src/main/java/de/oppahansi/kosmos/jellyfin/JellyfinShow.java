package de.oppahansi.kosmos.jellyfin;

/**
 * A series item read from Jellyfin's /Items — {@code id} is Jellyfin's own ItemId, used to group
 * {@link JellyfinEpisode}s under the series they belong to (episodes don't carry their own TMDB id,
 * only the parent series does).
 */
public record JellyfinShow(String id, String name, Integer year, String tmdbId, String path) {}
