package de.oppahansi.kosmos.metadata.anilist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AniListSearchResponse(Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Data(Page Page) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Page(List<AniListMedia> media) {}
}
