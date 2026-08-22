package de.oppahansi.kosmos.media.dto;

import java.util.List;
import java.util.UUID;

/** One show/anime's completeness — see {@code SeasonPassService}. */
public record SeasonPassEntry(
    UUID mediaItemId,
    String title,
    String posterPath,
    String contentType,
    List<SeasonPassSeason> seasons) {}
