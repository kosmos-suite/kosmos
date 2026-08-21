package de.oppahansi.kosmos.media.dto;

import java.util.UUID;

/**
 * Assigns or clears (null) the quality profile that makes a movie eligible for automatic search.
 */
public record UpdateMovieQualityProfileRequest(UUID qualityProfileId) {}
