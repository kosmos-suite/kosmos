package de.oppahansi.kosmos.calendar;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One dated thing to show on the calendar — a movie's release date, or one show/anime episode's air
 * date. {@code seasonNumber}/{@code episodeNumber} are null for a movie entry.
 */
public record CalendarEntry(
    UUID mediaItemId,
    String contentType,
    String title,
    LocalDate date,
    boolean monitored,
    Integer seasonNumber,
    Integer episodeNumber,
    String posterPath) {}
