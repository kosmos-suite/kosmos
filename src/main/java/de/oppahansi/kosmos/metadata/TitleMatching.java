package de.oppahansi.kosmos.metadata;

import java.util.Locale;

/**
 * Loose title comparison shared by anything that has to decide "is this the same title" from
 * nothing but two free-text strings — originally {@code JellyfinSyncService}'s anime-vs-show
 * classifier, now also {@code ImportMatchService}'s manual-import auto-match. Deliberately case/
 * punctuation-insensitive substring containment rather than an exact match: real-world titling
 * varies a lot (subtitle suffixes, romaji vs English, a release-group tag glued to the front), and
 * this only needs to catch titles with *no* relationship at all, not penalize minor wording
 * differences.
 */
public final class TitleMatching {

  private TitleMatching() {}

  /**
   * Either title being empty/unparseable doesn't count as a mismatch on its own — the caller always
   * has some other signal (year, season/episode numbers, ...) to fall back on.
   */
  public static boolean looselyMatch(String a, String b) {
    String normA = normalize(a);
    String normB = normalize(b);
    if (normA.isEmpty() || normB.isEmpty()) {
      return true;
    }
    return normA.contains(normB) || normB.contains(normA);
  }

  /**
   * The strict half — see callers for why only this (not {@link #looselyMatch}) is trusted to
   * excuse a disagreeing corroborating signal (year, episode count, ...).
   */
  public static boolean exactlyMatch(String a, String b) {
    String normA = normalize(a);
    String normB = normalize(b);
    return !normA.isEmpty() && normA.equals(normB);
  }

  public static String normalize(String title) {
    return title == null ? "" : title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }
}
