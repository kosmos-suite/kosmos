package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.CustomFormat;
import de.oppahansi.kosmos.parsing.RuleSpecification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates real TRaSH-Guides Radarr custom-format definitions (MIT-licensed) into Kosmos {@link
 * CustomFormat} rows.
 *
 * <p>Kosmos's {@link RuleSpecification} models {@code title}, {@code source}, {@code releaseGroup},
 * {@code resolution}, and {@code remux} (Radarr's "Quality Modifier: Remux" — the only {@code
 * QualityModifierSpecification} value any real in-scope definition ever uses, verified against all
 * 168 non-language-specific published formats). {@code LanguageSpecification} and {@code
 * IndexerFlagSpecification} aren't modeled yet — the former needs real language detection in {@code
 * ReleaseParser} first (a parsing feature, not a translation-table entry — see the roadmap's Phase
 * 11), the latter needs per-search-result indexer metadata Kosmos doesn't capture from Torznab
 * responses today. A definition using either (or any other unmodeled implementation) is skipped
 * rather than imported lossily, since silently dropping a required condition would change what the
 * format actually matches — see the skipped-reasons list in {@link TrashImportResult}.
 */
@ApplicationScoped
public class TrashGuidesImportService {

  /**
   * Radarr's {@code Source} enum id → Kosmos's canonical source name (parsing.Source). Verified
   * empirically against the real definitions rather than trusted from memory: every {@code
   * SourceSpecification} value actually found across all 241 published formats was one of these
   * four. WEBDL(7) and WEBRIP(8) both collapse to Kosmos's single "Web" source, which is coarser
   * than Radarr's split — an intentional simplification, not a bug.
   */
  private static final Map<Integer, String> SOURCE_ID_TO_KOSMOS =
      Map.of(
          5, "DVD",
          7, "Web",
          8, "Web",
          9, "Blu-ray");

  /**
   * Radarr's QualityModifier enum id → Kosmos boolean-ish "remux" field. Only REMUX(5) is ever used
   * by a real in-scope definition; every other value is treated as unsupported.
   */
  private static final int QUALITY_MODIFIER_REMUX = 5;

  private static final Set<String> SUPPORTED_IMPLEMENTATIONS =
      Set.of(
          "ReleaseTitleSpecification",
          "ReleaseGroupSpecification",
          "SourceSpecification",
          "ResolutionSpecification",
          "QualityModifierSpecification");

  @Inject TrashGuidesClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Transactional
  public TrashImportResult importAll() {
    TrashFetchResult fetched = client.fetchAll();
    int created = 0;
    int updated = 0;
    List<String> skipped = new ArrayList<>();
    for (String failedFilename : fetched.failedFilenames()) {
      skipped.add(failedFilename + " (fetch failed after retries)");
    }

    for (TrashCustomFormatDefinition definition : fetched.definitions()) {
      String unsupportedReason = unsupportedReason(definition);
      if (unsupportedReason != null) {
        skipped.add(definition.name() + " (" + unsupportedReason + ")");
        continue;
      }

      List<RuleSpecification> translated = translate(definition);
      String ruleJson = writeRule(translated);
      if (ruleJson == null) {
        skipped.add(definition.name() + " (rule serialization failed)");
        continue;
      }

      var existing = CustomFormat.<CustomFormat>find("trashId", definition.trashId());
      CustomFormat customFormat = existing.firstResultOptional().orElse(null);
      if (customFormat == null) {
        customFormat = new CustomFormat();
        customFormat.trashId = definition.trashId();
        created++;
      } else {
        updated++;
      }
      customFormat.name = definition.name();
      customFormat.score = definition.defaultScore();
      customFormat.rule = ruleJson;
      customFormat.persist();
    }

    return new TrashImportResult(created, updated, skipped);
  }

  private String unsupportedReason(TrashCustomFormatDefinition definition) {
    for (TrashSpecification spec : definition.specifications()) {
      if (!SUPPORTED_IMPLEMENTATIONS.contains(spec.implementation())) {
        return "uses unsupported " + spec.implementation();
      }
      if (spec.implementation().equals("SourceSpecification")
          && !SOURCE_ID_TO_KOSMOS.containsKey(sourceId(spec))) {
        return "uses unrecognized source id " + spec.fields().value();
      }
      if (spec.implementation().equals("QualityModifierSpecification")
          && numericValue(spec) != QUALITY_MODIFIER_REMUX) {
        return "uses unrecognized quality modifier id " + spec.fields().value();
      }
    }
    return null;
  }

  private List<RuleSpecification> translate(TrashCustomFormatDefinition definition) {
    return definition.specifications().stream().map(this::translate).toList();
  }

  private RuleSpecification translate(TrashSpecification spec) {
    return switch (spec.implementation()) {
      case "ReleaseTitleSpecification" ->
          new RuleSpecification(
              "title", "regex", (String) spec.fields().value(), spec.negate(), spec.required());
      case "ReleaseGroupSpecification" ->
          new RuleSpecification(
              "releaseGroup",
              "regex",
              (String) spec.fields().value(),
              spec.negate(),
              spec.required());
      case "SourceSpecification" ->
          new RuleSpecification(
              "source",
              "equals",
              SOURCE_ID_TO_KOSMOS.get(sourceId(spec)),
              spec.negate(),
              spec.required());
      case "ResolutionSpecification" ->
          new RuleSpecification(
              "resolution", "equals", numericValue(spec) + "p", spec.negate(), spec.required());
      case "QualityModifierSpecification" ->
          new RuleSpecification("remux", "equals", "true", spec.negate(), spec.required());
      default ->
          throw new IllegalStateException("Unsupported implementation: " + spec.implementation());
    };
  }

  private int sourceId(TrashSpecification spec) {
    return numericValue(spec);
  }

  private int numericValue(TrashSpecification spec) {
    return ((Number) spec.fields().value()).intValue();
  }

  private String writeRule(List<RuleSpecification> specs) {
    try {
      return objectMapper.writeValueAsString(specs);
    } catch (Exception e) {
      return null;
    }
  }
}
