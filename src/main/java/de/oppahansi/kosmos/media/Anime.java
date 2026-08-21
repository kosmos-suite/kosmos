package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.parsing.QualityProfile;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Anime-specific attributes for a {@link MediaItem}, sharing its primary key — same pattern as
 * {@link Movie}/{@link Show}. No {@code Season} equivalent between this and {@link AnimeEpisode}:
 * unlike TMDB, AniDB doesn't model seasons either — a new cour of a long-running show is normally
 * its own separate anime entry there, not a season of the existing one.
 */
@Entity
@Table(name = "anime")
public class Anime extends PanacheEntityBase {

  @Id
  @JdbcTypeCode(SqlTypes.VARCHAR)
  public UUID mediaItemId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(length = 4000)
  public String overview;

  @Column(name = "poster_path", length = 500)
  public String posterPath;

  @Column(name = "backdrop_path", length = 500)
  public String backdropPath;

  /** AniDB's own status string verbatim (e.g. "Ongoing", "Finished Airing") — not an enum. */
  @Column(length = 30)
  public String status;

  @Column(name = "episode_count_total")
  public Integer episodeCountTotal;

  @ManyToOne
  @JoinColumn(name = "quality_profile_id")
  public QualityProfile qualityProfile;
}
