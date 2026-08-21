package de.oppahansi.kosmos.jellyfin;

/**
 * Result of a successful AuthenticateByName call. accessToken is a per-user token Jellyfin accepts
 * anywhere an X-Emby-Token API key would go, so onboarding via a user's own credentials never needs
 * a separately-issued admin API key.
 */
public record JellyfinAuthResult(String userId, String name, boolean isAdmin, String accessToken) {}
