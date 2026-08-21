package de.oppahansi.kosmos.auth;

import de.oppahansi.kosmos.auth.dto.CreateUserRequest;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class UserService {

  private static final Set<String> ROLES = Set.of("ADMIN", "USER");

  public List<User> listAll() {
    return User.listAll();
  }

  public Optional<User> findById(UUID id) {
    return User.findByIdOptional(id);
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
    user.displayName = request.displayName();
    user.passwordHash = BcryptUtil.bcryptHash(request.password());
    user.role = role;
    user.enabled = true;
    user.createdAt = Instant.now();
    user.persist();
    return user;
  }
}
