package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** A named set of {@link CustomFormat} rules with a minimum score for release acceptance. */
@Entity
@Table(name = "quality_profile")
public class QualityProfile extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  @Column(name = "cutoff_score", nullable = false)
  public int cutoffScore;

  /**
   * Radarr's Delay Profile, simplified: 0 (default) grabs the best qualifying release the moment
   * {@code AutomaticSearchJob} finds it, matching every profile's behavior before this existed. A
   * positive value holds off grabbing until a candidate has been the best one seen for at least
   * this long, giving a better release time to show up on a later search — see {@link
   * de.oppahansi.kosmos.downloads.PendingCandidate}. Only meaningfully bites when the job's own
   * schedule interval is shorter than this delay; Radarr's own protocol-specific split (Usenet vs.
   * Torrent delay) isn't modeled since Kosmos doesn't distinguish indexer protocol today.
   */
  @Column(name = "grab_delay_minutes", nullable = false)
  public int grabDelayMinutes;

  /**
   * A candidate scoring at or above this grabs immediately regardless of {@link #grabDelayMinutes}
   * — Radarr's "bypass if highest quality" escape hatch. Null means no bypass; every candidate
   * waits out the full delay.
   */
  @Column(name = "bypass_score")
  public Integer bypassScore;

  @ManyToMany
  @JoinTable(
      name = "quality_profile_format",
      joinColumns = @JoinColumn(name = "quality_profile_id"),
      inverseJoinColumns = @JoinColumn(name = "custom_format_id"))
  public Set<CustomFormat> customFormats = new HashSet<>();
}
