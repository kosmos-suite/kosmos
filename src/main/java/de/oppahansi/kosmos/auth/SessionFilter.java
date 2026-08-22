package de.oppahansi.kosmos.auth;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves the session cookie (if any) into {@link CurrentUser} before the resource method runs.
 * Deliberately NOT {@code @PreMatching} — that runs on Vert.x's IO thread, where the blocking
 * Hibernate lookup below isn't allowed; a regular filter runs after routing has already decided
 * this is a blocking resource method and dispatched to a worker thread.
 *
 * <p>{@code kosmos.auth.dev-bypass} (only ever set under the {@code %dev} profile — see
 * application.properties, never a valid key in {@code %prod}) skips the cookie/session lookup
 * entirely and logs every request in as the first admin account, so local development doesn't need
 * a login round trip on every fresh database/browser session.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SessionFilter implements ContainerRequestFilter {

  public static final String COOKIE_NAME = "kosmos_session";

  @Inject CurrentUser currentUser;

  @ConfigProperty(name = "kosmos.auth.dev-bypass", defaultValue = "false")
  boolean devBypass;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (devBypass) {
      User.<User>find("role", "ADMIN").firstResultOptional().ifPresent(currentUser::set);
      return;
    }
    Cookie cookie = requestContext.getCookies().get(COOKIE_NAME);
    if (cookie == null) {
      return;
    }
    Session session = Session.<Session>findById(cookie.getValue());
    if (session != null && !session.isExpired()) {
      currentUser.set(session.user);
    }
  }
}
