package de.oppahansi.kosmos.jellyfin;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A Jellyfin "tvshows" item {@code JellyfinSyncService} couldn't confidently route to {@code Show}
 * or {@code Anime} — see the migration comment on this table for why. {@code anilistId} is
 * Jellyfin's own reported id (if any), kept only as a hint shown to the user; it's the same id that
 * failed to resolve, so it isn't reused to auto-create anything.
 */
@Entity
@Table(name = "unclassified_show")
public class UnclassifiedShow extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "jellyfin_server_id")
  public JellyfinServer jellyfinServer;

  @Column(name = "jellyfin_item_id", nullable = false, length = 200)
  public String jellyfinItemId;

  @Column(nullable = false, length = 500)
  public String name;

  public Integer year;

  @Column(name = "tmdb_id", nullable = false, length = 200)
  public String tmdbId;

  @Column(name = "anilist_id", length = 200)
  public String anilistId;

  @Column(nullable = false, length = 1000)
  public String path;

  /** Short machine code for why this couldn't be auto-classified — see {@link Reason}. */
  @Column(nullable = false, length = 40)
  public String reason;

  /** JSON array of {@code SeasonEpisodeCount} — see the migration's own comment on this column. */
  @Column(name = "seasons_json", length = 2000)
  public String seasonsJson;

  @Column(name = "detected_at", nullable = false)
  public Instant detectedAt;

  public enum Reason {
    /**
     * An anime match was found (Jellyfin's own id, or Fribb's reverse lookup) but AniList had no
     * data for it.
     */
    ANILIST_MATCH_UNCONFIRMED,
    /**
     * AniList had data, but its own reported start year and this item's title only partly agree
     * with what Jellyfin/TMDB report — could be a real match with a divergent title/year, or a
     * spurious one (Jellyfin's own anime-metadata plugin has been observed writing an AniList id
     * with zero title relationship to the real item, apparently from a full-text description match
     * gone wrong).
     */
    ANILIST_MATCH_AMBIGUOUS
  }
}
