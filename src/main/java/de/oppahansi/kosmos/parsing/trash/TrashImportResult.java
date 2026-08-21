package de.oppahansi.kosmos.parsing.trash;

import java.util.List;

/** Outcome of a {@link TrashGuidesImportService#importAll()} run. */
public record TrashImportResult(int created, int updated, List<String> skipped) {}
