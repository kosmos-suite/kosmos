package de.oppahansi.kosmos.parsing.dto;

import de.oppahansi.kosmos.parsing.CustomFormat;
import java.util.UUID;

public record CustomFormatResponse(UUID id, String name, int score, String rule, String trashId) {

  public static CustomFormatResponse from(CustomFormat customFormat) {
    return new CustomFormatResponse(
        customFormat.id,
        customFormat.name,
        customFormat.score,
        customFormat.rule,
        customFormat.trashId);
  }
}
