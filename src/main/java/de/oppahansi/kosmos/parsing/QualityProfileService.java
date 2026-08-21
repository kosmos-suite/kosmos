package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.QualityProfileRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class QualityProfileService {

  public List<QualityProfile> listAll() {
    return QualityProfile.listAll();
  }

  public Optional<QualityProfile> findById(UUID id) {
    return QualityProfile.findByIdOptional(id);
  }

  /**
   * Null in, null out; a non-null id that doesn't resolve is a client error, not a missing value.
   */
  public QualityProfile resolveOrThrow(UUID id) {
    if (id == null) {
      return null;
    }
    return findById(id)
        .orElseThrow(() -> new BadRequestException("Unknown quality profile id: " + id));
  }

  @Transactional
  public QualityProfile create(QualityProfileRequest request) {
    QualityProfile profile = new QualityProfile();
    apply(profile, request);
    profile.persist();
    return profile;
  }

  @Transactional
  public Optional<QualityProfile> update(UUID id, QualityProfileRequest request) {
    return findById(id).map(profile -> apply(profile, request));
  }

  /**
   * Fetches the managed entity first (rather than a bulk deleteById) so Hibernate clears the
   * quality_profile_format join rows for this profile as part of removing the owning side of
   * the @ManyToMany, instead of leaving orphaned join rows or hitting an FK violation.
   */
  @Transactional
  public boolean delete(UUID id) {
    return findById(id)
        .map(
            profile -> {
              profile.delete();
              return true;
            })
        .orElse(false);
  }

  private QualityProfile apply(QualityProfile profile, QualityProfileRequest request) {
    profile.name = request.name();
    profile.cutoffScore = request.cutoffScore();
    profile.customFormats = resolveCustomFormats(request.customFormatIds());
    return profile;
  }

  private Set<CustomFormat> resolveCustomFormats(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return new HashSet<>();
    }
    Set<CustomFormat> formats = new HashSet<>();
    for (UUID id : ids) {
      CustomFormat format =
          CustomFormat.<CustomFormat>findByIdOptional(id)
              .orElseThrow(() -> new BadRequestException("Unknown custom format id: " + id));
      formats.add(format);
    }
    return formats;
  }
}
