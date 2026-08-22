package de.oppahansi.kosmos.calendar;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A minimal RFC 5545 writer — just enough VEVENT structure for a read-only feed a calendar app
 * polls (Google/Apple Calendar, Radarr/Sonarr's own "subscribe by URL" feature). No external
 * library: the format is a handful of colon-delimited lines, not worth a dependency for. Doesn't
 * fold long lines (RFC 5545 §3.1's 75-octet limit) — every real-world client tolerates long lines
 * in practice, and Kosmos titles are short enough that this is unlikely to ever matter.
 */
final class IcsWriter {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  private IcsWriter() {}

  static String write(List<CalendarEntry> entries) {
    StringBuilder sb = new StringBuilder();
    sb.append("BEGIN:VCALENDAR\r\n");
    sb.append("VERSION:2.0\r\n");
    sb.append("PRODID:-//Kosmos//Calendar//EN\r\n");
    sb.append("CALSCALE:GREGORIAN\r\n");
    String stamp = TIMESTAMP.format(Instant.now());
    for (CalendarEntry entry : entries) {
      sb.append("BEGIN:VEVENT\r\n");
      sb.append("UID:").append(entry.mediaItemId()).append("@kosmos\r\n");
      sb.append("DTSTAMP:").append(stamp).append("\r\n");
      sb.append("DTSTART;VALUE=DATE:").append(DATE.format(entry.date())).append("\r\n");
      sb.append("SUMMARY:").append(escape(entry.title())).append("\r\n");
      sb.append("END:VEVENT\r\n");
    }
    sb.append("END:VCALENDAR\r\n");
    return sb.toString();
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
  }
}
