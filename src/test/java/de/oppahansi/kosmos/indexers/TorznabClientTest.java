package de.oppahansi.kosmos.indexers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.oppahansi.kosmos.indexers.dto.TorznabResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TorznabClientTest {

  private static final String SAMPLE_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <rss version="2.0" xmlns:torznab="http://torznab.com/schemas/2015/feed">
        <channel>
          <title>Test Indexer</title>
          <item>
            <title>Inception.2010.1080p.BluRay.x264-GROUP</title>
            <guid>https://example.com/details/123</guid>
            <pubDate>Mon, 01 Jan 2024 00:00:00 +0000</pubDate>
            <enclosure url="https://example.com/download/123.torrent" length="8589934592" type="application/x-bittorrent"/>
            <torznab:attr name="seeders" value="42"/>
            <torznab:attr name="peers" value="50"/>
            <torznab:attr name="infohash" value="abcdef0123456789"/>
          </item>
          <item>
            <title>The.Matrix.1999.2160p.WEB-DL.x265-GROUP</title>
            <guid>https://example.com/details/456</guid>
            <pubDate>Tue, 02 Jan 2024 12:30:00 +0000</pubDate>
            <enclosure url="https://example.com/download/456.torrent" length="17179869184" type="application/x-bittorrent"/>
            <torznab:attr name="seeders" value="7"/>
            <torznab:attr name="peers" value="2"/>
          </item>
        </channel>
      </rss>
      """;

  private final TorznabClient client = new TorznabClient();

  @Test
  void parsesAllItemsFromResponse() {
    List<TorznabResult> results = client.parse(SAMPLE_RESPONSE);
    assertEquals(2, results.size());
  }

  @Test
  void parsesFieldsOfFirstItem() {
    TorznabResult first = client.parse(SAMPLE_RESPONSE).get(0);

    assertEquals("Inception.2010.1080p.BluRay.x264-GROUP", first.title());
    assertEquals("https://example.com/download/123.torrent", first.downloadUrl());
    assertEquals(8589934592L, first.sizeBytes());
    assertEquals(42, first.seeders());
    assertEquals(50, first.peers());
    assertEquals(Instant.parse("2024-01-01T00:00:00Z"), first.publishedAt());
  }

  @Test
  void parsesItemWithoutInfohashAttr() {
    TorznabResult second = client.parse(SAMPLE_RESPONSE).get(1);

    assertEquals(7, second.seeders());
    assertEquals(2, second.peers());
    assertEquals(17179869184L, second.sizeBytes());
  }

  @Test
  void buildsSearchUrlWithEncodedQuery() {
    String url = client.buildSearchUrl("https://indexer.example/api", "key123", "The Matrix");

    assertTrue(url.startsWith("https://indexer.example/api?t=search&apikey=key123&q="));
    assertTrue(url.contains("The+Matrix"));
  }

  @Test
  void returnsEmptyListForResponseWithNoItems() {
    String empty =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"><channel><title>Empty</title></channel></rss>
        """;

    assertEquals(0, client.parse(empty).size());
  }
}
