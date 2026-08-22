package de.oppahansi.kosmos.library.dto;

import java.util.List;
import java.util.UUID;

public record CommitImportRequest(List<Item> items) {

  public record Item(String sourcePath, UUID mediaItemId) {}
}
