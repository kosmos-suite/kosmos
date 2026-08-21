package de.oppahansi.kosmos.metadata.thexem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * The real response shape of {@code GET /map/all}, confirmed live: {@code result} is {@code
 * "success"}/{@code "failure"} (a failure — e.g. no mapping for that id — comes back as a normal
 * 200 with an empty {@code data} array, not an HTTP error), and {@code message} is a human-readable
 * explanation not needed here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TheXemAllMapResponse(String result, List<TheXemEntry> data) {}
