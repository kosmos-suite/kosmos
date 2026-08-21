package de.oppahansi.kosmos.jellyfin;

import java.util.List;

/**
 * A top-level library folder read from Jellyfin's /Library/VirtualFolders. collectionType is
 * Jellyfin's own vocabulary ("movies", "tvshows", "boxsets", ...); locations are the real
 * filesystem paths backing it — a "boxsets"/collections library has none, since a collection is a
 * database-side grouping, not a folder on disk.
 */
public record JellyfinLibrary(
    String id, String name, String collectionType, List<String> locations) {}
