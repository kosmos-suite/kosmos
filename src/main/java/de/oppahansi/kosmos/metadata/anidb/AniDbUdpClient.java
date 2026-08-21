package de.oppahansi.kosmos.metadata.anidb;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * AniDB's UDP hash-lookup API — session-based AUTH, then FILE queries keyed on a file's ed2k hash +
 * size (computed by {@code library.HashService}), the same hash-based-identity approach Shoko
 * Server uses instead of filename parsing. Protocol details (command strings, response envelope,
 * the fmask/amask combination and the field order it fixes) are verified against adameste/anidbcli,
 * a real reference client on GitHub, rather than assumed from memory. Requires a registered UDP
 * client name/version and a real AniDB account, both configured via {@code
 * kosmos.metadata.anidb.*}.
 *
 * <p>Every request is serialized through {@link #send} with a hard {@value
 * #MIN_REQUEST_INTERVAL_MS}ms floor between UDP packets — AniDB's ban policy is strict (~1 req/2s,
 * karma-based bans) and, unlike a rate-limited HTTP API, there's no response header warning a
 * client it's about to get banned; the only safe move is never approaching the limit.
 */
@ApplicationScoped
public class AniDbUdpClient {

  private static final String HOST = "api.anidb.net";
  private static final int PORT = 9000;
  private static final int SOCKET_TIMEOUT_MS = 5_000;
  private static final int MAX_RETRIES = 3;
  private static final int MIN_REQUEST_INTERVAL_MS = 2_000;

  /**
   * fmask/amask exactly as used by adameste/anidbcli's {@code API_ENDPOINT_FILE} — this pair fixes
   * the response's pipe-delimited field order (fid/aid/eid/gid/lid/file_state/size/ed2k/... then
   * anime info). Only the first six fields are parsed (see {@link #parseFileMatch}); the value
   * still has to stay exactly this, since an unverified custom bitmask risks silently misreading
   * which field is which.
   */
  private static final String FILE_FMASK = "79FAFFE900";

  private static final String FILE_AMASK = "F2FCF0C0";

  private static final int LOGIN_ACCEPTED = 200;
  private static final int LOGIN_ACCEPTED_NEW_VERSION = 201;
  private static final int FILE_FOUND = 220;
  private static final int NO_SUCH_FILE = 320;
  private static final int INVALID_SESSION = 506;

  @ConfigProperty(name = "kosmos.metadata.anidb.username")
  Optional<String> username;

  @ConfigProperty(name = "kosmos.metadata.anidb.password")
  Optional<String> password;

  @ConfigProperty(name = "kosmos.metadata.anidb.client-name", defaultValue = "kosmos")
  String clientName;

  @ConfigProperty(name = "kosmos.metadata.anidb.client-version", defaultValue = "1")
  int clientVersion;

  private DatagramSocket socket;
  private volatile String session;
  private long lastRequestAtMillis;

  public boolean isConfigured() {
    return username.isPresent() && password.isPresent();
  }

  /** Empty when AniDB genuinely has no file matching this hash+size, not on any kind of error. */
  public synchronized Optional<AniDbFileMatch> lookupByHash(long sizeBytes, String ed2kHex) {
    requireConfigured();
    ensureAuthenticated();
    String command =
        "FILE size=%d&ed2k=%s&fmask=%s&amask=%s&s=%s"
            .formatted(sizeBytes, ed2kHex, FILE_FMASK, FILE_AMASK, session);
    Response response = sendWithReauth(command);
    if (response.code() == NO_SUCH_FILE) {
      return Optional.empty();
    }
    if (response.code() != FILE_FOUND) {
      throw new AniDbException(response.code(), response.data());
    }
    return Optional.of(parseFileMatch(response.data()));
  }

  @PreDestroy
  synchronized void logout() {
    if (session == null || socket == null) {
      return;
    }
    try {
      send("LOGOUT s=%s".formatted(session));
    } catch (IOException e) {
      // Best-effort — the session simply expires server-side if this doesn't land.
    } finally {
      socket.close();
      socket = null;
      session = null;
    }
  }

  private void requireConfigured() {
    if (!isConfigured()) {
      throw new IllegalStateException("kosmos.metadata.anidb.username/password are not configured");
    }
  }

  private void ensureAuthenticated() {
    if (session == null) {
      authenticate();
    }
  }

  private void authenticate() {
    String command =
        "AUTH user=%s&pass=%s&protover=3&client=%s&clientver=%d&enc=UTF8"
            .formatted(username.get(), password.get(), clientName, clientVersion);
    Response response;
    try {
      response = send(command);
    } catch (IOException e) {
      throw new UncheckedIOException("AniDB AUTH failed", e);
    }
    if (response.code() != LOGIN_ACCEPTED && response.code() != LOGIN_ACCEPTED_NEW_VERSION) {
      throw new AniDbException(response.code(), response.data());
    }
    session = response.data().split(" ", 2)[0];
  }

  /** Retries once after a fresh AUTH if the session AniDB had on file has since expired. */
  private Response sendWithReauth(String command) {
    try {
      Response response = send(command);
      if (response.code() == INVALID_SESSION) {
        session = null;
        authenticate();
        return send(command.replaceFirst("&s=\\S*$", "&s=" + session));
      }
      return response;
    } catch (IOException e) {
      throw new UncheckedIOException("AniDB UDP request failed", e);
    }
  }

  private Response send(String command) throws IOException {
    ensureSocket();
    waitForRateLimitWindow();
    byte[] payload = command.getBytes(StandardCharsets.UTF_8);
    DatagramPacket request = new DatagramPacket(payload, payload.length);
    byte[] buffer = new byte[8192];
    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

    IOException lastTimeout = null;
    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      try {
        lastRequestAtMillis = System.currentTimeMillis();
        socket.send(request);
        socket.receive(reply);
        String raw = new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8);
        return parseResponse(raw);
      } catch (SocketTimeoutException e) {
        lastTimeout = e;
      }
    }
    throw new IOException(
        "AniDB UDP request timed out after " + MAX_RETRIES + " attempts", lastTimeout);
  }

  private void ensureSocket() throws IOException {
    if (socket == null) {
      DatagramSocket s = new DatagramSocket();
      s.setSoTimeout(SOCKET_TIMEOUT_MS);
      s.connect(InetAddress.getByName(HOST), PORT);
      socket = s;
    }
  }

  private void waitForRateLimitWindow() {
    long elapsed = System.currentTimeMillis() - lastRequestAtMillis;
    if (lastRequestAtMillis > 0 && elapsed < MIN_REQUEST_INTERVAL_MS) {
      try {
        Thread.sleep(MIN_REQUEST_INTERVAL_MS - elapsed);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private Response parseResponse(String raw) {
    String trimmed = raw.stripTrailing();
    int code = Integer.parseInt(trimmed.substring(0, 3));
    String data = trimmed.length() > 4 ? trimmed.substring(4) : "";
    return new Response(code, data);
  }

  /**
   * Field order fixed by {@link #FILE_FMASK}/{@link #FILE_AMASK}: fid, aid, eid, gid, lid,
   * file_state, size, ed2k, then more file fields and anime info this client doesn't parse.
   */
  private AniDbFileMatch parseFileMatch(String data) {
    String[] lines = data.split("\n", 2);
    String[] fields = lines[lines.length - 1].split("\\|", -1);
    return new AniDbFileMatch(fields[0], fields[1], fields[2], fields[3], fields[5]);
  }

  private record Response(int code, String data) {}
}
