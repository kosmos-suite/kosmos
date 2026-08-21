package de.oppahansi.kosmos.scheduler.dto;

/** Body of {@code PUT /jobs/{name}} — the two fields a user can actually change about a job. */
public record UpdateScheduledJobRequest(boolean enabled, int intervalSeconds) {}
