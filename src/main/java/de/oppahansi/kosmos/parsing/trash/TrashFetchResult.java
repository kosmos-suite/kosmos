package de.oppahansi.kosmos.parsing.trash;

import java.util.List;

/** Outcome of {@link TrashGuidesClient#fetchAll()} — every in-scope file, fetched or not. */
public record TrashFetchResult(
    List<TrashCustomFormatDefinition> definitions, List<String> failedFilenames) {}
