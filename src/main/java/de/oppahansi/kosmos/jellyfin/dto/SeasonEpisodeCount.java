package de.oppahansi.kosmos.jellyfin.dto;

/**
 * How many episode files Jellyfin has under one season number for an {@code UnclassifiedShow} —
 * season 0 is specials, matching Jellyfin/TMDB convention. Shown on the "Needs Review" screen and
 * on the AniList search results next to it, since a season/episode breakdown often narrows down a
 * match faster than title/year/poster alone.
 */
public record SeasonEpisodeCount(int seasonNumber, int episodeCount) {}
