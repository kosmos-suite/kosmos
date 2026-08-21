package de.oppahansi.kosmos.parsing;

import java.util.regex.Pattern;

/** A recognized release-title token category backed by a precompiled alternation pattern. */
interface ReleaseToken {

  String canonicalName();

  Pattern pattern();

  /**
   * First token (in enum declaration order) whose pattern matches, or null. Shared by every parser
   * in this package.
   */
  static <T extends Enum<T> & ReleaseToken> String match(T[] tokens, String input) {
    for (T token : tokens) {
      if (token.pattern().matcher(input).find()) {
        return token.canonicalName();
      }
    }
    return null;
  }
}
