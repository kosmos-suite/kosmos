package de.oppahansi.kosmos.media.dto;

/** {@code seasonFolderEnabled} null resets to "use the global show naming settings". */
public record UpdateSeasonFolderRequest(Boolean seasonFolderEnabled) {}
