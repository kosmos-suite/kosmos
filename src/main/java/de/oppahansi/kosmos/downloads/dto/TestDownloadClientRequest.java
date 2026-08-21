package de.oppahansi.kosmos.downloads.dto;

public record TestDownloadClientRequest(
    String type, String baseUrl, String username, String password) {}
