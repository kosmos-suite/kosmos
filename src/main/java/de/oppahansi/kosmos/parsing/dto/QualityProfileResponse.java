package de.oppahansi.kosmos.parsing.dto;

import de.oppahansi.kosmos.parsing.QualityProfile;
import java.util.List;
import java.util.UUID;

public record QualityProfileResponse(
    UUID id, String name, int cutoffScore, List<CustomFormatResponse> customFormats) {

  public static QualityProfileResponse from(QualityProfile profile) {
    return new QualityProfileResponse(
        profile.id,
        profile.name,
        profile.cutoffScore,
        profile.customFormats.stream().map(CustomFormatResponse::from).toList());
  }
}
