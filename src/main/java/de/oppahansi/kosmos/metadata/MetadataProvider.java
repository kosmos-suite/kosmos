package de.oppahansi.kosmos.metadata;

import de.oppahansi.kosmos.metadata.dto.MetadataSearchResult;
import java.util.List;

/** A source of movie/show metadata, identified by a matching {@link Plugin} slug. */
public interface MetadataProvider {

  String pluginSlug();

  List<MetadataSearchResult> search(String query);
}
