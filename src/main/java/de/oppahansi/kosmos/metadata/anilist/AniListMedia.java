package de.oppahansi.kosmos.metadata.anilist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AniListMedia(
    int id,
    Title title,
    StartDate startDate,
    String description,
    CoverImage coverImage,
    String bannerImage,
    String status,
    Integer episodes,
    List<String> genres,
    Integer averageScore,
    Trailer trailer,
    Studios studios,
    Recommendations recommendations) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Title(String romaji, String english) {}

  /**
   * Only present when the query requests {@code trailer { id site } }. {@code site} is a lowercase
   * host name — {@code "youtube"} or {@code "dailymotion"}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Trailer(String id, String site) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record StartDate(Integer year) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record CoverImage(String large) {}

  /** Only present when the query requests {@code studios(isMain: true)}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Studios(List<Node> nodes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Node(String name) {}
  }

  /** Only present when the query requests {@code recommendations}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Recommendations(List<Node> nodes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Node(AniListMedia mediaRecommendation) {}
  }
}
