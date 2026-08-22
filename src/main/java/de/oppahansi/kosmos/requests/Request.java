package de.oppahansi.kosmos.requests;

import de.oppahansi.kosmos.auth.User;
import de.oppahansi.kosmos.common.KosmosEntity;
import de.oppahansi.kosmos.media.MediaItem;
import de.oppahansi.kosmos.parsing.QualityProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A title someone asked for. Denormalizes title/year/overview/posterPath from the metadata provider
 * at request time rather than joining through {@code externalId} at read time — the list needs to
 * render even for a declined request, and a decline shouldn't depend on a still-working TMDB call.
 * {@code mediaItemId} is null until approved, at which point it points at the real {@link
 * MediaItem} created (or already existing) for {@code externalId}.
 *
 * <p>{@code requestedBy} is null for a request {@code ImportListSyncJob} filed rather than a user —
 * {@code sourceListName} is set instead, and the Requests queue shows "List: &lt;name&gt;" in place
 * of a requester for these.
 */
@Entity
@Table(name = "request")
public class Request extends KosmosEntity {

  @ManyToOne
  @JoinColumn(name = "requested_by_id")
  public User requestedBy;

  @Column(name = "source_list_name", length = 200)
  public String sourceListName;

  @Column(name = "media_type", nullable = false, length = 10)
  public String mediaType;

  @Column(name = "external_id", nullable = false, length = 200)
  public String externalId;

  @Column(name = "plugin_slug", nullable = false, length = 100)
  public String pluginSlug;

  @Column(nullable = false, length = 500)
  public String title;

  public Integer year;

  @Column(length = 4000)
  public String overview;

  @Column(name = "poster_path", length = 500)
  public String posterPath;

  @Column(name = "backdrop_path", length = 500)
  public String backdropPath;

  /** The requester's preferred profile, if they picked one — admin can override it on approve. */
  @ManyToOne
  @JoinColumn(name = "quality_profile_id")
  public QualityProfile qualityProfile;

  @Column(nullable = false, length = 20)
  public String status;

  /** Decline reason, shown back to the requester. Never set for PENDING/APPROVED. */
  @Column(length = 1000)
  public String note;

  @ManyToOne
  @JoinColumn(name = "media_item_id")
  public MediaItem mediaItem;

  @Column(name = "requested_at", nullable = false)
  public Instant requestedAt;

  @ManyToOne
  @JoinColumn(name = "decided_by_id")
  public User decidedBy;

  @Column(name = "decided_at")
  public Instant decidedAt;
}
