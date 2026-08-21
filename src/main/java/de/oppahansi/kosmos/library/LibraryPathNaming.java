package de.oppahansi.kosmos.library;

import java.util.regex.Pattern;

/**
 * Shared "{Title} (Year)" on-disk directory naming for movies/shows/anime — kept in one place so
 * {@link ImportService} (which writes there) and {@link LocalArtworkService} (which reads back from
 * there for a local-poster fallback) can't silently diverge on where a title's files live.
 */
public final class LibraryPathNaming {

  private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

  private LibraryPathNaming() {}

  public static String sanitize(String name) {
    return ILLEGAL_FILENAME_CHARS.matcher(name).replaceAll("").trim();
  }

  public static String titleYear(String title, Integer year) {
    return sanitize(title) + " (" + year + ")";
  }
}
