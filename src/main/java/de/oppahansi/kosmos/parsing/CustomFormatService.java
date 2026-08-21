package de.oppahansi.kosmos.parsing;

import de.oppahansi.kosmos.parsing.dto.CustomFormatRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CustomFormatService {

  @Inject CustomFormatMatcher matcher;
  @Inject EntityManager entityManager;

  public List<CustomFormat> listAll() {
    return CustomFormat.listAll();
  }

  public Optional<CustomFormat> findById(UUID id) {
    return CustomFormat.findByIdOptional(id);
  }

  @Transactional
  public CustomFormat create(CustomFormatRequest request) {
    matcher.parseRule(request.rule());
    CustomFormat customFormat = new CustomFormat();
    apply(customFormat, request);
    customFormat.persist();
    return customFormat;
  }

  @Transactional
  public Optional<CustomFormat> update(UUID id, CustomFormatRequest request) {
    matcher.parseRule(request.rule());
    return findById(id).map(customFormat -> apply(customFormat, request));
  }

  /**
   * A CustomFormat still referenced by a QualityProfile's join table hits the FK constraint at the
   * DB level — that needs to surface as a 409, not a 500. Hibernate defers the DELETE's flush to
   * transaction commit by default, which happens after this method returns, so the explicit flush()
   * forces the constraint check to happen here, inside the try block.
   */
  @Transactional
  public boolean delete(UUID id) {
    try {
      boolean deleted = CustomFormat.deleteById(id);
      entityManager.flush();
      return deleted;
    } catch (RuntimeException e) {
      throw new WebApplicationException(
          "Custom format is still used by a quality profile", Response.Status.CONFLICT);
    }
  }

  private CustomFormat apply(CustomFormat customFormat, CustomFormatRequest request) {
    customFormat.name = request.name();
    customFormat.score = request.score();
    customFormat.rule = request.rule();
    return customFormat;
  }
}
