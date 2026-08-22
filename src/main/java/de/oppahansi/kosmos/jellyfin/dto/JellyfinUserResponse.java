package de.oppahansi.kosmos.jellyfin.dto;

import de.oppahansi.kosmos.jellyfin.JellyfinUser;

/** One account read live from Jellyfin's own /Users list, for the user-import selection UI. */
public record JellyfinUserResponse(String id, String name, boolean isAdmin) {

  public static JellyfinUserResponse from(JellyfinUser user) {
    return new JellyfinUserResponse(user.id(), user.name(), user.isAdmin());
  }
}
