package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One AniList cour of an {@link Anime} franchise — season/episode-order come from Fribb/anime-
 * lists' TMDB cross-reference, not AniList itself. See the {@code anime_season} table comment.
 */
@Entity
@Table(name = "anime_season")
public class AnimeSeason extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "anime_id")
  public Anime anime;

  @Column(name = "season_number", nullable = false)
  public int seasonNumber;

  @Column(nullable = false, length = 200)
  public String name;

  @Column(length = 4000)
  public String overview;

  @Column(name = "episode_count")
  public Integer episodeCount;

  @Column(name = "anilist_id", length = 200)
  public String anilistId;

  @Column(name = "absolute_offset", nullable = false)
  public int absoluteOffset;
}
