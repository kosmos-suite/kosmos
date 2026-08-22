package de.oppahansi.kosmos.metadata.anilist;

/**
 * Enough to create an {@code Anime} row — see {@link AniListMetadataProvider#fetchById}. {@code
 * startYear} is AniList's own reported first-air year — not persisted onto {@code Anime} (which
 * takes its year from the caller instead), but used by {@code JellyfinSyncService} as an
 * independent cross-check against a Jellyfin item's own reported year, since Jellyfin can tag two
 * physically distinct titles that merely share a name with the identical AniList id.
 */
public record AniListAnimeDetails(
    String title,
    String overview,
    String posterPath,
    String status,
    Integer episodeCount,
    Integer startYear) {}
