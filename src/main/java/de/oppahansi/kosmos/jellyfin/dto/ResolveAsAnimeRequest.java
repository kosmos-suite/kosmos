package de.oppahansi.kosmos.jellyfin.dto;

/**
 * The AniList id a user picked (see {@code MetadataResource#search}) to resolve an {@code
 * UnclassifiedShow} as anime.
 */
public record ResolveAsAnimeRequest(String anilistId) {}
