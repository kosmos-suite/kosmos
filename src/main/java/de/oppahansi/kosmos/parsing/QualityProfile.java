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

  @ManyToMany
  @JoinTable(
      name = "quality_profile_format",
      joinColumns = @JoinColumn(name = "quality_profile_id"),
      inverseJoinColumns = @JoinColumn(name = "custom_format_id"))
  public Set<CustomFormat> customFormats = new HashSet<>();
}
