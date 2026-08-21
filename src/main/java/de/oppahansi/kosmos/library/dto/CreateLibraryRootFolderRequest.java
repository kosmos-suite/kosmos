package de.oppahansi.kosmos.library.dto;

import java.util.List;

public record CreateLibraryRootFolderRequest(String path, List<String> contentTypes) {}
