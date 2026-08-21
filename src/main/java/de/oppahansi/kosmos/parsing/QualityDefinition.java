package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A hard min/max release size (in MB per minute of runtime) for one resolution+source combination —
 * a reject/allow gate applied before custom-format scoring, not an additive score. See {@link
 * QualityDefinitionService#checkSizeGate}.
 */
@Entity
@Table(name = "quality_definition")
public class QualityDefinition extends KosmosEntity {

  @Column(nullable = false, length = 20)
  public String resolution;

  @Column(nullable = false, length = 30)
  public String source;

  @Column(name = "min_mb_per_minute", nullable = false)
  public double minMbPerMinute;

  @Column(name = "max_mb_per_minute", nullable = false)
  public double maxMbPerMinute;
}
