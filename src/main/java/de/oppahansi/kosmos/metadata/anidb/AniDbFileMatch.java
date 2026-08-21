package de.oppahansi.kosmos.metadata.anidb;

/**
 * The identity fields of an {@code AUTH}/{@code FILE}-command hash match Kosmos actually needs:
 * {@code animeId}/{@code episodeId} are AniDB's own ids, the same ones {@code
 * metadata.fribb.FribbEntry#anidbId()} already cross-references, so a matched file links straight
 * back to an existing {@code Anime}/{@code AnimeEpisode} without a second lookup. AniDB's {@code
 * FILE} response carries far more (anime title/episode name/group name/...); those fields aren't
 * parsed here since Kosmos already sources anime/episode metadata from AniList/Fribb/TheXEM.
 */
public record AniDbFileMatch(
    String fileId, String animeId, String episodeId, String groupId, String fileState) {}
