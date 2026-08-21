package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A named scoring rule matched against a release's parsed and probed attributes. */
@Entity
@Table(name = "custom_format")
public class CustomFormat extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  @Column(nullable = false)
  public int score;

  /**
   * Rule definition as JSON. 16000 covers real TRaSH-Guides imports — some release-group-list
   * formats (e.g. "LQ") run past 10000 chars.
   */
  @Column(nullable = false, length = 16000)
  public String rule;

  /**
   * TRaSH-Guides {@code trash_id}, set only when imported from there; null for hand-authored
   * formats.
   */
  @Column(name = "trash_id", length = 64)
  public String trashId;
}
