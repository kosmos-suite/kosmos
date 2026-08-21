package de.oppahansi.kosmos.parsing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class CustomFormatMatcher {

  @Inject ObjectMapper objectMapper;

  /**
   * Parses and validates a {@link CustomFormat#rule} JSON string, throwing on any unknown
   * field/matchType.
   */
  public List<RuleSpecification> parseRule(String rule) {
    List<RuleSpecification> specs;
    try {
      specs = objectMapper.readValue(rule, new TypeReference<List<RuleSpecification>>() {});
    } catch (Exception e) {
      throw new BadRequestException("rule must be a JSON array of specifications");
    }
    for (RuleSpecification spec : specs) {
      if (!RuleSpecification.FIELDS.contains(spec.field())) {
        throw new BadRequestException("Unknown rule field: " + spec.field());
      }
      if (!RuleSpecification.MATCH_TYPES.contains(spec.matchType())) {
        throw new BadRequestException("Unknown rule matchType: " + spec.matchType());
      }
    }
    return specs;
  }

  public boolean matches(CustomFormat format, ParsedRelease release) {
    return matches(parseRule(format.rule), release);
  }

  public boolean matches(List<RuleSpecification> specs, ParsedRelease release) {
    boolean hasOptional = specs.stream().anyMatch(s -> !s.required());
    boolean anyOptionalMatched = false;

    for (RuleSpecification spec : specs) {
      boolean matched = evaluate(spec, release);
      if (spec.required()) {
        if (!matched) {
          return false;
        }
      } else if (matched) {
        anyOptionalMatched = true;
      }
    }
    return !hasOptional || anyOptionalMatched;
  }

  private boolean evaluate(RuleSpecification spec, ParsedRelease release) {
    String actual = fieldValue(spec.field(), release);
    boolean matched = test(spec.matchType(), spec.value(), actual);
    return spec.negate() ? !matched : matched;
  }

  private boolean test(String matchType, String expected, String actual) {
    if (actual == null) {
      return false;
    }
    if ("regex".equals(matchType)) {
      return Pattern.compile(expected, Pattern.CASE_INSENSITIVE).matcher(actual).find();
    }
    return actual.equalsIgnoreCase(expected);
  }

  private String fieldValue(String field, ParsedRelease release) {
    return switch (field) {
      case "title" -> release.title();
      case "year" -> release.year() == null ? null : release.year().toString();
      case "resolution" -> release.resolution();
      case "source" -> release.source();
      case "videoCodec" -> release.videoCodec();
      case "audioCodec" -> release.audioCodec();
      case "edition" -> release.edition();
      case "releaseGroup" -> release.releaseGroup();
      case "proper" -> String.valueOf(release.proper());
      case "repack" -> String.valueOf(release.repack());
      default -> null;
    };
  }
}
