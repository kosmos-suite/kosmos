package de.oppahansi.kosmos.jellyfin.dto;

/**
 * Outcome of one library sync run. linked = existing Kosmos movie/show/anime, newly-recorded file.
 * created = brand-new movie/show/anime. Show and anime fields mirror the movie ones one level up
 * (per-series, not per-file — a series is "linked" if any new episode file was matched to it).
 * Anime is counted separately from shows even though both come from the same Jellyfin "tvshows"
 * item type — see {@code JellyfinSyncService#resolveAnimeMatch}. episodeFilesLinked covers both.
 */
public record JellyfinLibrarySyncResult(
    int scanned,
    int linked,
    int created,
    int skippedNoTmdbId,
    int alreadySynced,
    int showsScanned,
    int showsLinked,
    int showsCreated,
    int showsSkippedNoTmdbId,
    int showsAlreadySynced,
    int animeLinked,
    int animeCreated,
    int animeAlreadySynced,
    int episodeFilesLinked) {}
