package de.oppahansi.kosmos.jellyfin.dto;

import de.oppahansi.kosmos.jellyfin.JellyfinLibrary;

public record JellyfinLibraryResponse(String id, String name, String collectionType) {

  public static JellyfinLibraryResponse from(JellyfinLibrary library) {
    return new JellyfinLibraryResponse(library.id(), library.name(), library.collectionType());
  }
}
