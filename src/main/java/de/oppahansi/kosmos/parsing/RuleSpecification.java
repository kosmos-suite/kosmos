package de.oppahansi.kosmos.parsing;

import java.util.Set;

/**
 * One condition inside a {@link CustomFormat}'s rule JSON, matched against a {@link
 * de.oppahansi.kosmos.parsing.dto.ParsedRelease} field.
 *
 * <p>Matching model mirrors Radarr/TRaSH-Guides custom formats (confirmed against real definitions
 * fetched from TRaSH-Guides/Guides): every {@code required} specification in a format must match,
 * and if the format has any non-required specifications, at least one of those must also match.
 * {@code field}/{@code value} are Kosmos-native (they address {@link
 * de.oppahansi.kosmos.parsing.dto.ParsedRelease}'s own fields directly) rather than Radarr's
 * internal numeric quality-ID encoding, which needs its own translation step — see the TRaSH-Guides
 * import item on the roadmap.
 */
public record RuleSpecification(
    String field, String matchType, String value, boolean negate, boolean required) {

  static final Set<String> FIELDS =
      Set.of(
          "title",
          "year",
          "resolution",
          "source",
          "videoCodec",
          "audioCodec",
          "edition",
          "releaseGroup",
          "proper",
          "repack");

  static final Set<String> MATCH_TYPES = Set.of("equals", "regex");
}
