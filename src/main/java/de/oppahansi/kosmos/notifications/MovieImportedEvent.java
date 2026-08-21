package de.oppahansi.kosmos.notifications;

/** Fired after a movie's import transaction actually commits — see NotificationService. */
public record MovieImportedEvent(String title, Integer year) {}
