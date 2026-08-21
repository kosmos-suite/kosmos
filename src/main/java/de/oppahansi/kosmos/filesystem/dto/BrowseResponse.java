package de.oppahansi.kosmos.filesystem.dto;

import java.util.List;

public record BrowseResponse(String path, String parentPath, List<DirectoryEntry> directories) {}
