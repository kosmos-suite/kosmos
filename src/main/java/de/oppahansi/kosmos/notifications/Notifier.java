package de.oppahansi.kosmos.notifications;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

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

  /** Comma-separated {@link NotificationEventType} names — null/blank means every event. */
  @Column(name = "enabled_events", length = 200)
  public String enabledEvents;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  public boolean wantsEvent(NotificationEventType type) {
    if (enabledEvents == null || enabledEvents.isBlank()) {
      return true;
    }
    return Arrays.asList(enabledEvents.split(",")).contains(type.name());
  }

  public static String joinEvents(List<NotificationEventType> types) {
    return types == null || types.isEmpty()
        ? null
        : String.join(",", types.stream().map(Enum::name).toList());
  }
}
