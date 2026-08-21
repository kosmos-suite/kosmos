package de.oppahansi.kosmos.downloads.dto;

/** Payload for registering a download client. {@code type} defaults to "QBITTORRENT" when blank. */
public record CreateDownloadClientRequest(
    String name, String type, String baseUrl, String username, String password, String category) {}
