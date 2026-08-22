package de.oppahansi.kosmos.backup;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure filename/JDBC-URL parsing pulled out of {@link BackupService} so it's testable without CDI.
 */
final class BackupFilenames {

  // Trailing (?:\?.*)? tolerates a query string — Quarkus Dev Services' auto-provisioned URL
  // always has one (e.g. "?loggerLevel=OFF"), which %prod's own fixed URL never does.
  private static final Pattern JDBC_URL =
      Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?");
  private static final Pattern BACKUP_FILENAME = Pattern.compile("kosmos-[0-9T-]+\\.dump");
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

  private BackupFilenames() {}

  static String newFilename(Instant now) {
    return "kosmos-" + TIMESTAMP.format(now) + ".dump";
  }

  static boolean isBackupFilename(String name) {
    return BACKUP_FILENAME.matcher(name).matches();
  }

  record JdbcTarget(String host, String port, String database) {}

  static Optional<JdbcTarget> parseJdbcUrl(String jdbcUrl) {
    Matcher matcher = JDBC_URL.matcher(jdbcUrl);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    String port = matcher.group(2) != null ? matcher.group(2) : "5432";
    return Optional.of(new JdbcTarget(matcher.group(1), port, matcher.group(3)));
  }
}
