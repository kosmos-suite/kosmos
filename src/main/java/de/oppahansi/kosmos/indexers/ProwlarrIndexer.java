package de.oppahansi.kosmos.indexers;

/** An indexer as configured in Prowlarr's own /api/v1/indexer list. */
public record ProwlarrIndexer(int id, String name, boolean enabled) {}
