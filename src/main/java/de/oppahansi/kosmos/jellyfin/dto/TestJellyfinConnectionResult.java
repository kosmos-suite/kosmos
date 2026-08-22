package de.oppahansi.kosmos.jellyfin.dto;

/**
 * Always a 200 — {@code ok} carries success/failure rather than an HTTP error status, since a
 * failed reachability check is an expected outcome the "Add server" modal displays inline, not a
 * server error. {@code message} is a ready-to-show summary either way.
 */
public record TestJellyfinConnectionResult(boolean ok, String message, int libraryCount) {}
