package de.oppahansi.kosmos.metadata.anilist;

/** Enough to create an {@code Anime} row — see {@link AniListMetadataProvider#fetchById}. */
public record AniListAnimeDetails(
    String title, String overview, String posterPath, String status, Integer episodeCount) {}
