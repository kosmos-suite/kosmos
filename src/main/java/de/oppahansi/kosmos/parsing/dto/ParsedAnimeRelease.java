package de.oppahansi.kosmos.parsing.dto;

/**
 * Structured attributes extracted from a raw fansub release title — a separate shape from {@link
 * ParsedRelease}, not an extension of it: the grammar is different enough (leading bracketed group,
 * ` - NN ` episode markers, batch ranges, absolute numbering with no season concept) that forcing
 * it through the scene-release parser's assumptions produces wrong results.
 */
public record ParsedAnimeRelease(
    /** The unmodified raw release title, same convention as {@link ParsedRelease#title()}. */
    String title,
    /** The leading {@code [Group]} tag — null if the title doesn't start with a bracket. */
    String releaseGroup,
    /**
     * Best-effort show title, trimmed of the group tag and everything from the episode marker on.
     */
    String showTitle,
    /**
     * Null for a batch release (see {@code episodeRangeEnd}) or when no episode marker was found.
     */
    Integer episodeNumber,
    /**
     * Set only for a batch release ({@code (01-12)}-style range) — the last episode in the range.
     */
    Integer episodeRangeEnd,
    /** The {@code v2}/{@code v3} revision suffix on the episode marker, if present. */
    Integer version,
    boolean batch,
    boolean dualAudio,
    String resolution,
    String source,
    String videoCodec,
    String audioCodec,
    /** The 8-hex-digit CRC32 embedded in most fansub filenames, lowercased — null if absent. */
    String crc32) {}
