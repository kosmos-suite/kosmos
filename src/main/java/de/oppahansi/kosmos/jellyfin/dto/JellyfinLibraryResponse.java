package de.oppahansi.kosmos.jellyfin.dto;

import de.oppahansi.kosmos.jellyfin.JellyfinLibrary;
import java.util.List;

public record JellyfinLibraryResponse(
    String id, String name, String collectionType, List<String> locations) {

  public static JellyfinLibraryResponse from(JellyfinLibrary library) {
    return new JellyfinLibraryResponse(
        library.id(), library.name(), library.collectionType(), library.locations());
  }
}
