package de.oppahansi.kosmos.indexers;

import de.oppahansi.kosmos.indexers.dto.TorznabResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Queries Torznab-compatible indexers and parses their RSS search results. */
@ApplicationScoped
public class TorznabClient {

  private static final String TORZNAB_NS = "http://torznab.com/schemas/2015/feed";

  private final HttpClient httpClient = HttpClient.newHttpClient();

  public List<TorznabResult> search(String baseUrl, String apiKey, String query)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(buildSearchUrl(baseUrl, apiKey, query)))
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return parse(response.body());
  }

  String buildSearchUrl(String baseUrl, String apiKey, String query) {
    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
    return "%s?t=search&apikey=%s&q=%s".formatted(baseUrl, apiKey, encodedQuery);
  }

  List<TorznabResult> parse(String xml) {
    Document document = parseDocument(xml);
    NodeList items = document.getElementsByTagName("item");
    List<TorznabResult> results = new ArrayList<>();

    for (int i = 0; i < items.getLength(); i++) {
      Element item = (Element) items.item(i);
      results.add(parseItem(item));
    }
    return results;
  }

  private TorznabResult parseItem(Element item) {
    String title = textContent(item, "title");
    Instant publishedAt = parsePubDate(textContent(item, "pubDate"));

    Element enclosure = firstElement(item, "enclosure");
    String downloadUrl = enclosure != null ? enclosure.getAttribute("url") : null;
    long sizeBytes =
        enclosure != null && !enclosure.getAttribute("length").isBlank()
            ? Long.parseLong(enclosure.getAttribute("length"))
            : 0L;

    Integer seeders = torznabAttr(item, "seeders");
    Integer peers = torznabAttr(item, "peers");

    return new TorznabResult(title, downloadUrl, sizeBytes, seeders, peers, publishedAt);
  }

  private Integer torznabAttr(Element item, String name) {
    NodeList attrs = item.getElementsByTagNameNS(TORZNAB_NS, "attr");
    for (int i = 0; i < attrs.getLength(); i++) {
      Element attr = (Element) attrs.item(i);
      if (name.equals(attr.getAttribute("name"))) {
        String value = attr.getAttribute("value");
        return value.isBlank() ? null : Integer.valueOf(value);
      }
    }
    return null;
  }

  private Instant parsePubDate(String pubDate) {
    if (pubDate == null || pubDate.isBlank()) {
      return null;
    }
    return DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDate, Instant::from);
  }

  private String textContent(Element parent, String tagName) {
    Element element = firstElement(parent, tagName);
    return element != null ? element.getTextContent() : null;
  }

  private Element firstElement(Element parent, String tagName) {
    NodeList nodes = parent.getElementsByTagName(tagName);
    return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
  }

  private Document parseDocument(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new StringReader(xml)));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new IllegalArgumentException("Invalid Torznab response", e);
    }
  }
}
