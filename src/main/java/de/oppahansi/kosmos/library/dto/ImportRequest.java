package de.oppahansi.kosmos.library.dto;

/**
 * A completed download's location — either a single video file, or a torrent's content directory.
 */
public record ImportRequest(String sourcePath) {}
