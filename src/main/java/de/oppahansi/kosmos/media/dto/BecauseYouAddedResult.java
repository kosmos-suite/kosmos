package de.oppahansi.kosmos.media.dto;

import java.util.List;

/** Backs Discover/Home's "Because You Added {title}" row. */
public record BecauseYouAddedResult(String basedOnTitle, List<DiscoverItem> items) {}
