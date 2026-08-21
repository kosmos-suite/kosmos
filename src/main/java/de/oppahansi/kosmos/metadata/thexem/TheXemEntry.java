package de.oppahansi.kosmos.metadata.thexem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One row of TheXEM's scene/AniDB numbering map — verified live against a real divergent case
 * (anidb id 144): {@code anidb.episode} is AniDB's own per-entry episode number, {@code
 * scene.absolute} is the continuous count fansub filenames actually carry, and the two aren't
 * always the same number. The {@code tvdb} block the real API also returns isn't modeled — nothing
 * in Kosmos reads it, same convention as {@link de.oppahansi.kosmos.metadata.fribb.FribbEntry}'s
 * dropped {@code movie} field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TheXemEntry(Numbering scene, Numbering anidb) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Numbering(Integer season, Integer episode, Integer absolute) {}
}
