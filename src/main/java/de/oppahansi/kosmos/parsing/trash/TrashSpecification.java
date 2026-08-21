package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One condition inside a {@link TrashCustomFormatDefinition}. {@code fields.value} is a regex
 * string for {@code ReleaseTitleSpecification}/{@code ReleaseGroupSpecification}, or a numeric
 * Radarr {@code Source} enum id for {@code SourceSpecification} — see {@link
 * TrashGuidesImportService} for the translation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrashSpecification(
    String name, String implementation, boolean negate, boolean required, Fields fields) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Fields(Object value) {}
}
