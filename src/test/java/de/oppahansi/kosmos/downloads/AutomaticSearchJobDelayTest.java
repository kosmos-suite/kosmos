package de.oppahansi.kosmos.downloads;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AutomaticSearchJobDelayTest {

  @Test
  void notElapsedBeforeTheDeadline() {
    Instant firstSeen = Instant.parse("2026-01-01T00:00:00Z");
    Instant now = firstSeen.plusSeconds(59 * 60);
    assertFalse(AutomaticSearchJob.delayElapsed(firstSeen, 60, now));
  }

  @Test
  void elapsedExactlyAtTheDeadline() {
    Instant firstSeen = Instant.parse("2026-01-01T00:00:00Z");
    Instant now = firstSeen.plusSeconds(60 * 60);
    assertTrue(AutomaticSearchJob.delayElapsed(firstSeen, 60, now));
  }

  @Test
  void elapsedWellPastTheDeadline() {
    Instant firstSeen = Instant.parse("2026-01-01T00:00:00Z");
    Instant now = firstSeen.plusSeconds(3 * 60 * 60);
    assertTrue(AutomaticSearchJob.delayElapsed(firstSeen, 60, now));
  }
}
