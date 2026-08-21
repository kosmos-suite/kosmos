package de.oppahansi.kosmos.metadata.anilist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record AniListMedia(
    int id,
    Title title,
    StartDate startDate,
    String description,
    CoverImage coverImage,
    String bannerImage,
    String status,
    Integer episodes) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Title(String romaji, String english) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record StartDate(Integer year) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record CoverImage(String large) {}
}
