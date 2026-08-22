package de.oppahansi.kosmos.importlists.dto;

import java.util.UUID;

public record UpdateImportListRequest(
    String name, boolean enabled, boolean trusted, UUID qualityProfileId) {}
