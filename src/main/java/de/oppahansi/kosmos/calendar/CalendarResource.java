package de.oppahansi.kosmos.calendar;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Path("/calendar")
@Produces(MediaType.APPLICATION_JSON)
public class CalendarResource {

  @Inject CalendarService calendarService;

  @GET
  public List<CalendarEntry> get(
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @QueryParam("monitoredOnly") boolean monitoredOnly) {
    LocalDate today = LocalDate.now();
    LocalDate resolvedFrom =
        from != null
            ? LocalDate.parse(from)
            : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate resolvedTo = to != null ? LocalDate.parse(to) : resolvedFrom.plusDays(6);
    return calendarService.between(resolvedFrom, resolvedTo, monitoredOnly);
  }
}
