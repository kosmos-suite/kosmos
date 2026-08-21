package de.oppahansi.kosmos.jellyfin;

/** A top-level library folder read from Jellyfin's /Library/VirtualFolders. */
public record JellyfinLibrary(String id, String name, String collectionType) {}
