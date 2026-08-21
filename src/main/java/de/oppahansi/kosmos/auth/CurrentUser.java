package de.oppahansi.kosmos.auth;

import jakarta.enterprise.context.RequestScoped;
import java.util.Optional;

/** Populated per-request by {@link SessionFilter} from the session cookie, if any. */
@RequestScoped
public class CurrentUser {

  private User user;

  void set(User user) {
    this.user = user;
  }

  public Optional<User> get() {
    return Optional.ofNullable(user);
  }

  public boolean isAdmin() {
    return user != null && user.isAdmin();
  }
}
