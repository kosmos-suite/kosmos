package de.oppahansi.kosmos.media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MinimumAvailabilityTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
  private static final LocalDate PAST = LocalDate.of(2026, 1, 1);
  private static final LocalDate FUTURE = LocalDate.of(2027, 1, 1);

  @Test
  void announcedIsAlwaysAvailable() {
    assertTrue(MinimumAvailability.ANNOUNCED.isAvailable(FUTURE, FUTURE, TODAY));
    assertTrue(MinimumAvailability.ANNOUNCED.isAvailable(null, null, TODAY));
  }

  @Test
  void inCinemasGatesOnReleaseDateOnly() {
    assertTrue(MinimumAvailability.IN_CINEMAS.isAvailable(PAST, FUTURE, TODAY));
    assertFalse(MinimumAvailability.IN_CINEMAS.isAvailable(FUTURE, PAST, TODAY));
  }

  @Test
  void inCinemasFailsOpenOnUnknownDate() {
    assertTrue(MinimumAvailability.IN_CINEMAS.isAvailable(null, null, TODAY));
  }

  @Test
  void releasedPrefersDigitalDateOverTheatrical() {
    assertFalse(MinimumAvailability.RELEASED.isAvailable(PAST, FUTURE, TODAY));
    assertTrue(MinimumAvailability.RELEASED.isAvailable(FUTURE, PAST, TODAY));
  }

  @Test
  void releasedFallsBackToTheatricalWhenNoDigitalDate() {
    assertTrue(MinimumAvailability.RELEASED.isAvailable(PAST, null, TODAY));
    assertFalse(MinimumAvailability.RELEASED.isAvailable(FUTURE, null, TODAY));
  }

  @Test
  void releasedFailsOpenWhenBothDatesUnknown() {
    assertTrue(MinimumAvailability.RELEASED.isAvailable(null, null, TODAY));
  }

  @Test
  void todayItselfCountsAsAvailable() {
    assertTrue(MinimumAvailability.IN_CINEMAS.isAvailable(TODAY, null, TODAY));
    assertTrue(MinimumAvailability.RELEASED.isAvailable(null, TODAY, TODAY));
  }
}
