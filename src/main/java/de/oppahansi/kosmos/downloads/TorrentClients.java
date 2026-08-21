package de.oppahansi.kosmos.downloads;

/**
 * Resolves a configured {@link DownloadClient} row to the {@link TorrentClient} that speaks its
 * protocol.
 */
final class TorrentClients {

  private TorrentClients() {}

  static TorrentClient forConfig(DownloadClient client) {
    return switch (client.type) {
      case "TRANSMISSION" -> new TransmissionClient(client.baseUrl);
      case "DELUGE" -> new DelugeClient(client.baseUrl);
      case "SABNZBD" -> new SabnzbdClient(client.baseUrl);
      case "NZBGET" -> new NzbgetClient(client.baseUrl);
      default -> new QbittorrentClient(client.baseUrl);
    };
  }
}
