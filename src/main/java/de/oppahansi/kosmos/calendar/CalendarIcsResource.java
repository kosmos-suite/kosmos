package de.oppahansi.kosmos.calendar;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;

/**
 * Separate resource (rather than a method on {@link CalendarResource}) purely so the path can be
 * the literal {@code /calendar.ics} a calendar app expects to subscribe to — JAX-RS joins a
 * class-level and method-level {@code @Path} with a {@code /}, so there's no way to get that exact
 * sibling path from a method under {@code /calendar}.
 *
 * <p>Deliberately not behind auth, same as most of the rest of this API (see {@code SessionFilter}
 * — Kosmos doesn't gate reads at the filter level) — that's exactly what lets an external calendar
 * app (Google/Apple Calendar) poll it by URL alone, the same "subscribe" model Radarr/Sonarr's own
 * iCal feeds use.
 */
@Path("/calendar.ics")
public class CalendarIcsResource {

  @Inject CalendarService calendarService;

  /**
   * Wider window than {@link CalendarResource#get} — a subscribed feed is polled periodically, not
   * loaded fresh each time.
   */
  @GET
  @Produces("text/calendar")
  public Response get(@QueryParam("monitoredOnly") boolean monitoredOnly) {
    LocalDate today = LocalDate.now();
    List<CalendarEntry> entries =
        calendarService.between(today.minusDays(7), today.plusDays(42), monitoredOnly);
    return Response.ok(IcsWriter.write(entries)).build();
  }
}
