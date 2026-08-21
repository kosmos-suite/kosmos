package de.oppahansi.kosmos.metadata.anidb;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class AniDbUdpClientTest {

  @Test
  void throwsWhenCredentialsAreNotConfigured() {
    AniDbUdpClient client = new AniDbUdpClient();
    client.username = Optional.empty();
    client.password = Optional.empty();

    assertThrows(IllegalStateException.class, () -> client.lookupByHash(1, "0".repeat(32)));
  }

  /**
   * Genuinely authenticates against the real AniDB UDP API and performs one real FILE lookup —
   * skipped, not faked, without real credentials configured (same {@code
   * KOSMOS_METADATA_TMDB_API_KEY}-style gating as {@code WasmTmdbMetadataProviderTest}). Looks up a
   * hash AniDB has certainly never seen (32 zero characters isn't a hash any real file could
   * produce) rather than a real file's, so this stays a single AUTH+FILE request pair — enough to
   * prove the whole round trip (session login, request signing, response parsing) works without
   * needing a known-real file on hand, and well inside AniDB's strict per-request rate limit.
   */
  @Test
  @EnabledIfEnvironmentVariable(named = "KOSMOS_METADATA_ANIDB_USERNAME", matches = ".+")
  void authenticatesAndReturnsEmptyForAHashAniDbHasNeverSeen() {
    AniDbUdpClient client = new AniDbUdpClient();
    client.username = Optional.of(System.getenv("KOSMOS_METADATA_ANIDB_USERNAME"));
    client.password = Optional.of(System.getenv("KOSMOS_METADATA_ANIDB_PASSWORD"));
    client.clientName = "kosmos";
    client.clientVersion = 1;

    Optional<AniDbFileMatch> match = client.lookupByHash(1, "0".repeat(32));

    assertTrue(match.isEmpty());
    client.logout();
  }
}
