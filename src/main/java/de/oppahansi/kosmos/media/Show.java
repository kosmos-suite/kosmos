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
 * Show-specific attributes for a {@link MediaItem}, sharing its primary key — same pattern as
 * {@link Movie}. {@code qualityProfile} is carried now for schema forward-compatibility, but
 * nothing reads it yet: season/episode-aware release parsing and automatic search for TV are still
 * separate, unstarted roadmap items (a whole show is never itself a grabbable release the way a
 * movie is — Sonarr grabs per-episode or per-season-pack).
 */
@Entity
@Table(name = "show")
public class Show extends PanacheEntityBase {

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

  /** TMDB's show status string verbatim (e.g. "Continuing", "Ended") — not modeled as an enum. */
  @Column(length = 30)
  public String status;

  @ManyToOne
  @JoinColumn(name = "quality_profile_id")
  public QualityProfile qualityProfile;

  /**
   * Per-show override of whether episodes land in a "Season N" subfolder — null means "use the
   * global show naming settings' season folder template" (the only behavior before this existed),
   * {@code false} means every episode goes straight in the series folder for this show regardless
   * of the global setting. See {@code ImportService#episodeTargetPath}.
   */
  @Column(name = "season_folder_enabled")
  public Boolean seasonFolderEnabled;
}
