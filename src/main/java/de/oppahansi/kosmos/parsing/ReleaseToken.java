package de.oppahansi.kosmos.parsing;

import java.util.regex.Matcher;
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

  /**
   * The earliest position any token matches at, or -1 if none do — {@code ReleaseParser}'s
   * cleanTitle extraction uses this to also cut before a resolution/source/codec marker, not just a
   * year or season/episode one, since a release with no year still has some technical marker
   * bounding the real title.
   */
  static <T extends Enum<T> & ReleaseToken> int earliestMatchStart(T[] tokens, String input) {
    int earliest = -1;
    for (T token : tokens) {
      Matcher matcher = token.pattern().matcher(input);
      if (matcher.find() && (earliest == -1 || matcher.start() < earliest)) {
        earliest = matcher.start();
      }
    }
    return earliest;
  }
}
