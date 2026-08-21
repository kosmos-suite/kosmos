package de.oppahansi.kosmos.auth.dto;

import de.oppahansi.kosmos.auth.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String displayName,
    String role,
    boolean jellyfinLinked,
    Instant createdAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.id,
        user.username,
        user.displayName,
        user.role,
        user.jellyfinServer != null,
        user.createdAt);
  }
}
