package de.oppahansi.kosmos.auth;

import de.oppahansi.kosmos.jellyfin.JellyfinAuthResult;
import de.oppahansi.kosmos.jellyfin.JellyfinClient;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ApplicationScoped
public class AuthService {

  private static final int SESSION_DAYS = 30;
  private final SecureRandom random = new SecureRandom();

  @Transactional
  public Optional<Session> login(String username, String password) {
    Optional<User> maybeUser = User.<User>find("username", username).firstResultOptional();
    if (maybeUser.isEmpty() || !maybeUser.get().enabled) {
      return Optional.empty();
    }
    User user = maybeUser.get();

    boolean authenticated =
        user.jellyfinServer != null
            ? authenticateViaJellyfin(user, password)
            : user.passwordHash != null && BcryptUtil.matches(password, user.passwordHash);
    if (!authenticated) {
      return Optional.empty();
    }

    Session session = new Session();
    session.token = generateToken();
    session.user = user;
    session.createdAt = Instant.now();
    session.expiresAt = Instant.now().plus(SESSION_DAYS, ChronoUnit.DAYS);
    session.persist();
    return Optional.of(session);
  }

  @Transactional
  public void logout(String token) {
    Session.deleteById(token);
  }

  private boolean authenticateViaJellyfin(User user, String password) {
    try {
      Optional<JellyfinAuthResult> result =
          new JellyfinClient(user.jellyfinServer.baseUrl).authenticate(user.username, password);
      if (result.isEmpty()) {
        return false;
      }
      // Keep the mirrored admin flag current — Jellyfin remains the source of truth for it.
      user.role = result.get().isAdmin() ? "ADMIN" : "USER";
      return true;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    StringBuilder hex = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }
}
