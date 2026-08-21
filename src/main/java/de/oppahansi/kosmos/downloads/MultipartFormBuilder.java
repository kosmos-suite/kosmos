package de.oppahansi.kosmos.downloads;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Hand-rolled {@code multipart/form-data} body builder — qBittorrent's and SABnzbd's {@code
 * addTorrentFile} (a directly-uploaded file, as opposed to {@code addTorrent}'s URL) each need one,
 * and previously duplicated the same boundary/field/file-part writing independently. Built by hand
 * rather than pulled in via a multipart library, since this is the only place in the app that needs
 * one.
 */
final class MultipartFormBuilder {

  private final String boundary = "KosmosBoundary" + UUID.randomUUID();
  private final ByteArrayOutputStream body = new ByteArrayOutputStream();

  String boundary() {
    return boundary;
  }

  MultipartFormBuilder field(String name, String value) {
    return part(name, null, null, value.getBytes(StandardCharsets.UTF_8));
  }

  MultipartFormBuilder file(String name, String filename, String contentType, byte[] content) {
    return part(name, filename, contentType, content);
  }

  private MultipartFormBuilder part(
      String name, String filename, String contentType, byte[] content) {
    try {
      body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
      String disposition =
          filename == null
              ? "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
              : "Content-Disposition: form-data; name=\""
                  + name
                  + "\"; filename=\""
                  + filename
                  + "\"\r\n";
      body.write(disposition.getBytes(StandardCharsets.UTF_8));
      if (contentType != null) {
        body.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
      }
      body.write("\r\n".getBytes(StandardCharsets.UTF_8));
      body.write(content);
      body.write("\r\n".getBytes(StandardCharsets.UTF_8));
      return this;
    } catch (IOException e) {
      // ByteArrayOutputStream#write never actually throws — its signature just inherits
      // OutputStream's, so this is unreachable in practice.
      throw new UncheckedIOException(e);
    }
  }

  byte[] build() {
    try {
      body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      return body.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
