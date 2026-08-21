package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import de.oppahansi.kosmos.parsing.dto.QualityDefinitionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class QualityDefinitionService {

  public List<QualityDefinition> listAll() {
    return QualityDefinition.listAll();
  }

  public Optional<QualityDefinition> findById(UUID id) {
    return QualityDefinition.findByIdOptional(id);
  }

  @Transactional
  public QualityDefinition create(QualityDefinitionRequest request) {
    QualityDefinition definition = new QualityDefinition();
    apply(definition, request);
    definition.persist();
    return definition;
  }

  @Transactional
  public Optional<QualityDefinition> update(UUID id, QualityDefinitionRequest request) {
    return findById(id).map(definition -> apply(definition, request));
  }

  @Transactional
  public boolean delete(UUID id) {
    return QualityDefinition.deleteById(id);
  }

  private QualityDefinition apply(QualityDefinition definition, QualityDefinitionRequest request) {
    definition.resolution = request.resolution();
    definition.source = request.source();
    definition.minMbPerMinute = request.minMbPerMinute();
    definition.maxMbPerMinute = request.maxMbPerMinute();
    return definition;
  }

  /**
   * The hard reject/allow gate: null if the release passes (or can't be evaluated — no runtime
   * known yet, unrecognized resolution/source, or no definition configured for this
   * resolution+source pair), otherwise a human-readable reason it was rejected. Deliberately
   * returns "can't evaluate" as a pass rather than a reject — an unknown bound should never block a
   * release that might be perfectly fine.
   */
  public String checkSizeGate(ParsedRelease parsed, long sizeBytes, Integer runtimeMinutes) {
    if (runtimeMinutes == null || runtimeMinutes <= 0) {
      return null;
    }
    if (parsed.resolution() == null || parsed.source() == null) {
      return null;
    }
    Optional<QualityDefinition> definition =
        QualityDefinition.<QualityDefinition>find(
                "resolution = ?1 and source = ?2", parsed.resolution(), parsed.source())
            .firstResultOptional();
    if (definition.isEmpty()) {
      return null;
    }

    double mbPerMinute = sizeBytes / 1024.0 / 1024.0 / runtimeMinutes;
    QualityDefinition d = definition.get();
    // Locale.ROOT, not the JVM default — String.formatted() otherwise renders "%.1f" with a
    // comma decimal separator on a server running under a non-English locale, which is both
    // inconsistent with the rest of the app's English copy and awkward if a client ever parses
    // the number back out of this message.
    if (mbPerMinute < d.minMbPerMinute) {
      return String.format(
          Locale.ROOT,
          "%.1f MB/min is below the %.1f MB/min floor for %s %s — likely a fake or sample",
          mbPerMinute,
          d.minMbPerMinute,
          parsed.resolution(),
          parsed.source());
    }
    if (mbPerMinute > d.maxMbPerMinute) {
      return String.format(
          Locale.ROOT,
          "%.1f MB/min is above the %.1f MB/min ceiling for %s %s",
          mbPerMinute,
          d.maxMbPerMinute,
          parsed.resolution(),
          parsed.source());
    }
    return null;
  }
}
