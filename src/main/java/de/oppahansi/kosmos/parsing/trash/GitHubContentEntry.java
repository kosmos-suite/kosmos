package de.oppahansi.kosmos.parsing.trash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One entry from GitHub's "list directory contents" API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContentEntry(String name, @JsonProperty("download_url") String downloadUrl) {}
