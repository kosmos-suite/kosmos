package de.oppahansi.kosmos.notifications;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A configured notification target. Fields are used per {@code type}: DISCORD/WEBHOOK use {@code
 * url}; TELEGRAM uses {@code token} (bot token) and {@code target} (chat id).
 */
@Entity
@Table(name = "notifier")
public class Notifier extends KosmosEntity {

  @Column(nullable = false, length = 200)
  public String name;

  @Column(nullable = false, length = 20)
  public String type;

  @Column(length = 1000)
  public String url;

  @Column(length = 500)
  public String token;

  @Column(length = 200)
  public String target;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
