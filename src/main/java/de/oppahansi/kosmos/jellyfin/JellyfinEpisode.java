package de.oppahansi.kosmos.jellyfin;

/**
 * An episode item read from Jellyfin's /Items — {@code seriesId} matches {@link JellyfinShow#id()};
 * season/episode number is how it's matched against Kosmos's own TMDB-built episode tree, since
 * Jellyfin's episode-level ProviderIds aren't reliably populated the way the series' TMDB id is.
 */
public record JellyfinEpisode(
    String seriesId, Integer seasonNumber, Integer episodeNumber, String path) {}
