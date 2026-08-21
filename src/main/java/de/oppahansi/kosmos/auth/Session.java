package de.oppahansi.kosmos.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A logged-in session — the token itself is the primary key, not a generated UUID. */
@Entity
@Table(name = "user_session")
public class Session extends PanacheEntityBase {

  @Id
  @Column(length = 64)
  public String token;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  public User user;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  public boolean isExpired() {
    return expiresAt.isBefore(Instant.now());
  }
}
