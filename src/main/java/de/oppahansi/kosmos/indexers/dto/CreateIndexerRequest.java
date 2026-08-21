package de.oppahansi.kosmos.indexers.dto;

/** Payload for registering an indexer. */
public record CreateIndexerRequest(String name, String baseUrl, String apiKey) {}
