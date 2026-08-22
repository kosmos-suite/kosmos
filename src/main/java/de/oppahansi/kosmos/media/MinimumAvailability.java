package de.oppahansi.kosmos.media;

import java.time.LocalDate;

/**
 * Gates {@code AutomaticSearchJob} on a configurable milestone per {@link Movie} — the same
 * reasoning Radarr uses to avoid grabbing a cam-quality day-one release before a real one exists.
 * Never gates interactive/manual search — a human explicitly searching already knows what they're
 * getting.
 *
 * <p>An unknown date (TMDB hasn't published it yet) fails open, not closed — otherwise a title with
 * incomplete metadata would never be searched at all, which is worse than not gating.
 */
public enum MinimumAvailability {
  ANNOUNCED,
  IN_CINEMAS,
  RELEASED;

  public boolean isAvailable(LocalDate releaseDate, LocalDate digitalReleaseDate, LocalDate today) {
    return switch (this) {
      case ANNOUNCED -> true;
      case IN_CINEMAS -> releaseDate == null || !releaseDate.isAfter(today);
      case RELEASED -> {
        LocalDate gate = digitalReleaseDate != null ? digitalReleaseDate : releaseDate;
        yield gate == null || !gate.isAfter(today);
      }
    };
  }
}
