package de.oppahansi.kosmos.downloads.dto;

/** Payload for {@code PUT /download-clients/{id}/path-mapping}. Either field may be blank/null. */
public record UpdatePathMappingRequest(String remotePath, String localPath) {}
