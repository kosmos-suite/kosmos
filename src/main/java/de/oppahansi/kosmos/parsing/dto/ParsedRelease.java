package de.oppahansi.kosmos.parsing.dto;

/** Structured attributes extracted from a raw release title. */
public record ParsedRelease(
    /**
     * The unmodified raw release title (not just the movie-name portion) — this is what
     * RuleSpecification's "title" field regexes match against, matching Radarr/TRaSH-Guides'
     * "Release Title" convention where the whole scene name is fair game, not just the title up to
     * the year.
     */
    String title,
    Integer year,
    String resolution,
    String source,
    String videoCodec,
    String audioCodec,
    String edition,
    String releaseGroup,
    boolean proper,
    boolean repack,
    /** Null for a release with no season/episode marker at all (a movie, most commonly). */
    Integer seasonNumber,
    /** Null for a season-pack release ({@code seasonNumber} set, no specific episode). */
    Integer episodeNumber) {}
