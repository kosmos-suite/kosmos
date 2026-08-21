package de.oppahansi.kosmos.parsing;

import java.util.regex.Pattern;

/**
 * Recognized release editions. Canonical values and aliases are ported from guessit's edition
 * property dictionary (guessit-io/guessit, LGPL-3.0).
 */
enum Edition implements ReleaseToken {
  ULTIMATE("Ultimate", "ultimate-?edition"),
  CRITERION("Criterion", "cc", "criterion", "criterion-?edition"),
  COLLECTOR("Collector", "collector'?s?-?edition"),
  DELUXE("Deluxe", "deluxe", "deluxe-?edition"),
  REMASTERED("Remastered", "(?:4k-?)?remaster(?:ed)?"),
  RESTORED("Restored", "(?:4k-?)?restored?"),
  DIRECTORS_CUT("Director's Cut", "dc", "director'?s?-?cut(?:-?edition)?"),
  EXTENDED("Extended", "extended", "extended-?cut", "extended-?version"),
  THEATRICAL("Theatrical", "theatrical", "theatrical-?cut", "theatrical-?edition"),
  UNRATED("Unrated", "unrated"),
  UNCUT("Uncut", "uncut"),
  UNCENSORED("Uncensored", "uncensored"),
  IMAX("IMAX", "imax", "imax-?edition"),
  LIMITED("Limited", "limited", "limited-?edition"),
  SPECIAL("Special", "special-?edition", "se");

  private final String canonicalName;
  private final Pattern pattern;

  Edition(String canonicalName, String... aliases) {
    this.canonicalName = canonicalName;
    this.pattern =
        Pattern.compile("\\b(?:" + String.join("|", aliases) + ")\\b", Pattern.CASE_INSENSITIVE);
  }

  @Override
  public String canonicalName() {
    return canonicalName;
  }

  @Override
  public Pattern pattern() {
    return pattern;
  }
}
