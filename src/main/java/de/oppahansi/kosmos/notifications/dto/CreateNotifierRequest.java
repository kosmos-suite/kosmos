package de.oppahansi.kosmos.notifications.dto;

import java.util.List;

/**
 * Payload for registering a notifier. type: DISCORD | TELEGRAM | WEBHOOK. {@code enabledEvents}
 * empty/omitted means every event type.
 */
public record CreateNotifierRequest(
    String name,
    String type,
    String url,
    String token,
    String target,
    List<String> enabledEvents) {}
