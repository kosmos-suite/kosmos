package de.oppahansi.kosmos.importlists.dto;

import java.util.UUID;

public record CreateImportListRequest(
    String name, String sourceType, boolean trusted, UUID qualityProfileId) {}
