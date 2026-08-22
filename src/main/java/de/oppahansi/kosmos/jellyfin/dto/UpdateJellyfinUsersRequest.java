package de.oppahansi.kosmos.jellyfin.dto;

import java.util.List;

public record UpdateJellyfinUsersRequest(List<String> userIds) {}
