package de.oppahansi.kosmos.auth;

import de.oppahansi.kosmos.auth.dto.CreateUserRequest;
import de.oppahansi.kosmos.jellyfin.JellyfinAuthResult;
import de.oppahansi.kosmos.jellyfin.JellyfinClient;
import de.oppahansi.kosmos.jellyfin.JellyfinServer;
import de.oppahansi.kosmos.jellyfin.JellyfinServerService;
import de.oppahansi.kosmos.jellyfin.dto.CreateJellyfinServerRequest;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class UserService {

  private static final Set<String> ROLES = Set.of("ADMIN", "USER");

  @Inject JellyfinServerService jellyfinServerService;

  public List<User> listAll() {
    return User.listAll();
  }

  public Optional<User> findById(UUID id) {
    return User.findByIdOptional(id);
  }

  public boolean needsSetup() {
    return User.count() == 0;
  }

  /**
   * The very first user ever created bootstraps as ADMIN with no auth required — there is no admin
   * yet to have authorized it. Every user after that needs an existing admin as requester.
   */
  @Transactional
  public User createNative(CreateUserRequest request, Optional<User> requester) {
    boolean bootstrap = User.count() == 0;
    if (!bootstrap && (requester.isEmpty() || !requester.get().isAdmin())) {
      throw new ForbiddenException("Only an admin can create users");
    }
    if (User.find("username", request.username()).firstResultOptional().isPresent()) {
      throw new BadRequestException("Username already taken: " + request.username());
    }
    String role = bootstrap ? "ADMIN" : request.role() == null ? "USER" : request.role();
    if (!ROLES.contains(role)) {
      throw new BadRequestException("Unknown role: " + role);
    }

    User user = new User();
    user.username = request.username();
    user.displayName =
        request.displayName() == null || request.displayName().isBlank()
            ? request.username()
            : request.displayName();
    user.passwordHash = BcryptUtil.bcryptHash(request.password());
    user.role = role;
    user.enabled = true;
    user.createdAt = Instant.now();
    user.persist();
    return user;
  }

  /**
   * Bootstraps Kosmos from an existing Jellyfin server instead of a native account — verifies the
   * given credentials via Jellyfin itself (never stores the password) and requires the account to
   * be a Jellyfin administrator, since this becomes the Kosmos admin. Only ever allowed once, same
   * as {@link #createNative}'s bootstrap path.
   */
  @Transactional
  public User createFromJellyfinBootstrap(String serverUrl, String username, String password) {
    if (User.count() != 0) {
      throw new ForbiddenException("An admin account already exists");
    }
    if (serverUrl == null || serverUrl.isBlank()) {
      throw new BadRequestException("Server URL is required");
    }

    JellyfinAuthResult result;
    try {
      result =
          new JellyfinClient(serverUrl)
              .authenticate(username, password, "kosmos-server-sync")
              .orElse(null);
    } catch (IOException | InterruptedException e) {
      throw new BadRequestException("Could not reach that Jellyfin server: " + e.getMessage());
    }
    if (result == null) {
      throw new BadRequestException("Wrong username or password");
    }
    if (!result.isAdmin()) {
      throw new BadRequestException("Only a Jellyfin administrator can set up Kosmos this way");
    }

    JellyfinServer server =
        jellyfinServerService.create(
            new CreateJellyfinServerRequest(
                deriveServerName(serverUrl), serverUrl, result.accessToken()));

    User user = new User();
    user.username = result.name();
    user.displayName = result.name();
    user.jellyfinServer = server;
    user.jellyfinUserId = result.userId();
    user.role = "ADMIN";
    user.enabled = true;
    user.createdAt = Instant.now();
    user.persist();
    return user;
  }

  private String deriveServerName(String serverUrl) {
    try {
      String host = URI.create(serverUrl).getHost();
      return host == null ? "Jellyfin" : "Jellyfin (" + host + ")";
    } catch (IllegalArgumentException e) {
      return "Jellyfin";
    }
  }
}
