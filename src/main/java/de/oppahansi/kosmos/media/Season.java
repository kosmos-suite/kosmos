package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One season of a {@link Show}, as TMDB reports it — including season 0 ("Specials"). */
@Entity
@Table(name = "season")
public class Season extends KosmosEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "show_id")
  public Show show;

  @Column(name = "season_number", nullable = false)
  public int seasonNumber;

  @Column(nullable = false, length = 200)
  public String name;

  @Column(length = 4000)
  public String overview;

  @Column(name = "poster_path", length = 500)
  public String posterPath;

  @Column(name = "episode_count")
  public Integer episodeCount;
}
