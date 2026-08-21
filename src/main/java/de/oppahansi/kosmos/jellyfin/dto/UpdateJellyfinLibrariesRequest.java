package de.oppahansi.kosmos.jellyfin.dto;

import java.util.List;

public record UpdateJellyfinLibrariesRequest(List<String> libraryIds) {}
