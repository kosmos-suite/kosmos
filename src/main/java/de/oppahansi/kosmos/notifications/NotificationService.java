package de.oppahansi.kosmos.notifications;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A native Java equivalent of Apprise's "one call, many services" pattern rather than shelling out
 * to the Python tool — these are plain webhook POSTs, nothing Apprise's 100+ integrations do here
 * needs a subprocess. Observing {@code AFTER_SUCCESS} (not calling this synchronously from inside
 * the import transaction) means a notification only ever fires for an import that actually
 * committed, and a slow/unreachable notifier can't hold that transaction open.
 */
@ApplicationScoped
public class NotificationService {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  void onMovieImported(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) MovieImportedEvent event) {
    String title = event.year() == null ? event.title() : event.title() + " (" + event.year() + ")";
    notifyAll("Movie imported", title + " has been imported and is ready to watch.");
  }

  public void notifyAll(String title, String message) {
    for (Notifier notifier : Notifier.<Notifier>list("enabled", true)) {
      try {
        send(notifier, title, message);
      } catch (Exception e) {
        // One broken notifier (bad URL, unreachable service) must never affect the others.
      }
    }
  }

  private void send(Notifier notifier, String title, String message) throws Exception {
    switch (notifier.type) {
      case "DISCORD" ->
          post(notifier.url, "{\"content\":\"" + escape(title + ": " + message) + "\"}");
      case "TELEGRAM" ->
          post(
              "https://api.telegram.org/bot" + notifier.token + "/sendMessage",
              "{\"chat_id\":\""
                  + escape(notifier.target)
                  + "\",\"text\":\""
                  + escape(title + ": " + message)
                  + "\"}");
      case "WEBHOOK" ->
          post(
              notifier.url,
              "{\"title\":\"" + escape(title) + "\",\"message\":\"" + escape(message) + "\"}");
      default -> throw new IllegalStateException("Unknown notifier type: " + notifier.type);
    }
  }

  private void post(String url, String jsonBody) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
