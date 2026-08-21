package de.oppahansi.kosmos.library.dto;

/** source is "runtime", "env", or "unset". */
public record LibraryRootPathResponse(String rootPath, String source) {}
