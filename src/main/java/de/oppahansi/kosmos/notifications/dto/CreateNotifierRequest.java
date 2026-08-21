package de.oppahansi.kosmos.notifications.dto;

/** Payload for registering a notifier. type: DISCORD | TELEGRAM | WEBHOOK. */
public record CreateNotifierRequest(
    String name, String type, String url, String token, String target) {}
