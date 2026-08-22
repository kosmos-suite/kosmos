package de.oppahansi.kosmos.notifications;

/**
 * Every kind of thing a {@link Notifier} can fire on. Deliberately only the events that have a real
 * trigger point wired today (see {@link NotificationEvent}'s own doc) — Radarr/Sonarr's fuller set
 * (Upgrade, Rename, Title Added/Deleted, Health Issue/Restored, ...) is real roadmap territory, not
 * modeled here yet so this doesn't ship half-wired options with nothing behind them.
 */
public enum NotificationEventType {
  GRAB,
  IMPORT,
  BLOCKLIST
}
