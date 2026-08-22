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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Movie-specific attributes for a {@link MediaItem}, sharing its primary key. */
@Entity
@Table(name = "movie")
public class Movie extends PanacheEntityBase {

  @Id
  @JdbcTypeCode(SqlTypes.VARCHAR)
  public UUID mediaItemId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(name = "runtime_minutes")
  public Integer runtimeMinutes;

  @Column(length = 4000)
  public String overview;

  @Column(name = "poster_path", length = 500)
  public String posterPath;

  @Column(name = "backdrop_path", length = 500)
  public String backdropPath;

  /**
   * TMDB's theatrical release date — the single date {@code CalendarService} uses for a movie's
   * calendar entry. Not the fuller "Minimum Availability" model (separate cinema/physical/digital
   * dates plus an availability delay) Radarr has; that's its own, larger roadmap item.
   */
  @Column(name = "release_date")
  public LocalDate releaseDate;

  /**
   * Null means "not monitored" — {@code AutomaticSearchJob} only ever considers assigned movies.
   */
  @ManyToOne
  @JoinColumn(name = "quality_profile_id")
  public QualityProfile qualityProfile;
}
