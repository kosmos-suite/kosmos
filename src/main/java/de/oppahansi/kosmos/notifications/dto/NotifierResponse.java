package de.oppahansi.kosmos.notifications.dto;

import de.oppahansi.kosmos.notifications.Notifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * API representation of a {@link Notifier}. url/token are webhook credentials — never returned.
 * {@code enabledEvents} empty means every event type — see {@link Notifier#wantsEvent}.
 */
public record NotifierResponse(
    UUID id,
    String name,
    String type,
    boolean urlSet,
    boolean tokenSet,
    String target,
    boolean enabled,
    List<String> enabledEvents,
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
        notifier.enabledEvents == null || notifier.enabledEvents.isBlank()
            ? List.of()
            : Arrays.asList(notifier.enabledEvents.split(",")),
        notifier.createdAt);
  }
}
