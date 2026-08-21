package de.oppahansi.kosmos.requests.dto;

import java.util.UUID;

/**
 * Body for both approve and decline — {@code qualityProfileId} is approve-only, {@code note}
 * decline-only.
 */
public record DecideRequestRequest(UUID qualityProfileId, String note) {}
