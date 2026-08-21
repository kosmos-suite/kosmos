package de.oppahansi.kosmos.plugins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One {@code plugins/<slug>.json} file from the kosmos-plugin-registry repo — a discovery pointer
 * at a third-party plugin author's own repository and a pinned release, not the plugin's own
 * manifest. See that repo's {@code schema/entry.schema.json} for the authoritative field list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryEntry(
    String slug,
    String name,
    String description,
    String category,
    String publisher,
    String repository,
    String version,
    String checksum,
    String homepage) {}
