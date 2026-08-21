package de.oppahansi.kosmos.library.dto;

/**
 * Backs the Library sidebar's Collections section and disk-usage widget. {@code totalBytes} is null
 * when {@code kosmos.library.root-path} isn't configured — there's no disk to report on.
 */
public record LibraryStatsResponse(
    long movieCount, long seriesCount, long animeCount, long usedBytes, Long totalBytes) {}
