package de.oppahansi.kosmos.notifications;

import de.oppahansi.kosmos.notifications.dto.CreateNotifierRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class NotifierService {

  private static final Set<String> TYPES = Set.of("DISCORD", "TELEGRAM", "WEBHOOK");

  public List<Notifier> listAll() {
    return Notifier.listAll();
  }

  public Optional<Notifier> findById(UUID id) {
    return Notifier.findByIdOptional(id);
  }

  @Transactional
  public Notifier create(CreateNotifierRequest request) {
    if (!TYPES.contains(request.type())) {
      throw new BadRequestException("Unknown notifier type: " + request.type());
    }
    Notifier notifier = new Notifier();
    notifier.name = request.name();
    notifier.type = request.type();
    notifier.url = request.url();
    notifier.token = request.token();
    notifier.target = request.target();
    notifier.enabled = true;
    notifier.createdAt = Instant.now();
    notifier.persist();
    return notifier;
  }
}
