package de.oppahansi.kosmos.notifications.dto;

import de.oppahansi.kosmos.notifications.Notifier;
import java.time.Instant;
import java.util.UUID;

/** API representation of a {@link Notifier}. url/token are webhook credentials — never returned. */
public record NotifierResponse(
    UUID id,
    String name,
    String type,
    boolean urlSet,
    boolean tokenSet,
    String target,
    boolean enabled,
    Instant createdAt) {

  public static NotifierResponse from(Notifier notifier) {
    return new NotifierResponse(
        notifier.id,
        notifier.name,
        notifier.type,
        notifier.url != null && !notifier.url.isBlank(),
        notifier.token != null && !notifier.token.isBlank(),
        notifier.target,
        notifier.enabled,
        notifier.createdAt);
  }
}
