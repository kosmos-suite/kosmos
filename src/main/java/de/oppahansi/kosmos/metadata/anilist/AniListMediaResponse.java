package de.oppahansi.kosmos.metadata.anilist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record AniListMediaResponse(Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Data(AniListMedia Media) {}
}
