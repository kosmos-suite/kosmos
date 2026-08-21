package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** One TRaSH-Guides Radarr custom-format JSON definition (github.com/TRaSH-Guides/Guides). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrashCustomFormatDefinition(
    @JsonProperty("trash_id") String trashId,
    String name,
    @JsonProperty("trash_scores") Map<String, Integer> trashScores,
    List<TrashSpecification> specifications) {

  public int defaultScore() {
    return trashScores == null ? 0 : trashScores.getOrDefault("default", 0);
  }
}
