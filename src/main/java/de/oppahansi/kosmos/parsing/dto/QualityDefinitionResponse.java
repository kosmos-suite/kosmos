package de.oppahansi.kosmos.parsing.dto;

import de.oppahansi.kosmos.parsing.QualityDefinition;
import java.util.UUID;

public record QualityDefinitionResponse(
    UUID id, String resolution, String source, double minMbPerMinute, double maxMbPerMinute) {

  public static QualityDefinitionResponse from(QualityDefinition definition) {
    return new QualityDefinitionResponse(
        definition.id,
        definition.resolution,
        definition.source,
        definition.minMbPerMinute,
        definition.maxMbPerMinute);
  }
}
