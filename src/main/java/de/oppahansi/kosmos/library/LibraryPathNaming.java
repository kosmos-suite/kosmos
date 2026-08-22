package de.oppahansi.kosmos.library;

import java.util.regex.Pattern;

/**
 * Filesystem-safety for a rendered naming-template segment — stripping characters no common
 * filesystem allows in a name. Used by {@link
 * de.oppahansi.kosmos.library.naming.NamingTemplateEngine} so every folder/file name Kosmos writes,
 * however its template is configured, goes through the same final sanitization.
 */
public final class LibraryPathNaming {

  private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

  private LibraryPathNaming() {}

  public static String sanitize(String name) {
    return ILLEGAL_FILENAME_CHARS.matcher(name).replaceAll("").trim();
  }
}
