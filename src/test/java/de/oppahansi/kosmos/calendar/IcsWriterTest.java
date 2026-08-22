package de.oppahansi.kosmos.calendar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IcsWriterTest {

  @Test
  void writesOneVeventPerEntry() {
    UUID id = UUID.randomUUID();
    CalendarEntry entry =
        new CalendarEntry(id, "movie", "Belle", LocalDate.of(2026, 1, 17), true, null, null, null);

    String ics = IcsWriter.write(List.of(entry));

    assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
    assertTrue(ics.contains("BEGIN:VEVENT\r\n"));
    assertTrue(ics.contains("UID:" + id + "@kosmos\r\n"));
    assertTrue(ics.contains("DTSTART;VALUE=DATE:20260117\r\n"));
    assertTrue(ics.contains("SUMMARY:Belle\r\n"));
    assertTrue(ics.contains("END:VEVENT\r\n"));
    assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
  }

  @Test
  void escapesRfc5545SpecialCharactersInTitle() {
    CalendarEntry entry =
        new CalendarEntry(
            UUID.randomUUID(),
            "episode",
            "Show, Part; One \\ Two",
            LocalDate.of(2026, 1, 17),
            true,
            1,
            1,
            null);

    String ics = IcsWriter.write(List.of(entry));

    assertTrue(ics.contains("SUMMARY:Show\\, Part\\; One \\\\ Two\r\n"));
  }

  @Test
  void emptyEntryListStillProducesAValidEmptyCalendar() {
    String ics = IcsWriter.write(List.of());
    assertTrue(ics.contains("BEGIN:VCALENDAR"));
    assertTrue(ics.contains("END:VCALENDAR"));
    assertTrue(!ics.contains("VEVENT"));
  }
}
