package de.oppahansi.kosmos.auth;

import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.jellyfin.JellyfinServer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Either a native account ({@code passwordHash} set, {@code jellyfin*} null) or a Jellyfin-linked
 * account ({@code jellyfinServer}+{@code jellyfinUserId} set, {@code passwordHash} null — login
 * always proxies to that server, and {@code role} is kept in sync with its own
 * Policy.IsAdministrator flag on every successful login).
 */
@Entity
@Table(name = "app_user")
public class User extends KosmosEntity {

  @Column(nullable = false, unique = true, length = 100)
  public String username;

  @Column(name = "display_name", nullable = false, length = 200)
  public String displayName;

  @Column(name = "password_hash", length = 200)
  public String passwordHash;

  @ManyToOne
  @JoinColumn(name = "jellyfin_server_id")
  public JellyfinServer jellyfinServer;

  @Column(name = "jellyfin_user_id", length = 100)
  public String jellyfinUserId;

  @Column(nullable = false, length = 20)
  public String role;

  @Column(nullable = false)
  public boolean enabled;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  public boolean isAdmin() {
    return "ADMIN".equals(role);
  }
}
