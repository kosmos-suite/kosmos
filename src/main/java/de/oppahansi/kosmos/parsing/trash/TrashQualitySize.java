package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** TRaSH-Guides' real movie.json quality-size reference (docs/json/radarr/quality-size). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrashQualitySize(List<Entry> qualities) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Entry(String quality, double min, double max) {}
}
