package de.oppahansi.kosmos.jellyfin;

/**
 * A series item read from Jellyfin's /Items — {@code id} is Jellyfin's own ItemId, used to group
 * {@link JellyfinEpisode}s under the series they belong to (episodes don't carry their own TMDB id,
 * only the parent series does). {@code anilistId} is only populated when the server has an
 * anime-metadata plugin installed (e.g. Jellyfin.Plugin.AniList) that writes an {@code AniList}
 * ProviderId — when present, {@code JellyfinSyncService} trusts it over the Fribb/TMDB reverse
 * lookup, since it's a direct identification rather than a cross-reference guess.
 */
public record JellyfinShow(
    String id, String name, Integer year, String tmdbId, String anilistId, String path) {}
