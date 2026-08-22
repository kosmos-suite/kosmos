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
    /**
     * Best-effort movie/show name — everything before the first season/episode or year marker,
     * trimmed of separator punctuation. Falls back to {@code title} itself when no marker was found
     * to cut at. This is what a title-vs-library-catalog comparison (manual import's auto-match,
     * most notably) should read, never {@code title} — the raw release title.
     */
    String cleanTitle,
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
