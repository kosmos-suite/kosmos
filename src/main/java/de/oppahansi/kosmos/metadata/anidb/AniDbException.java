package de.oppahansi.kosmos.metadata.anidb;

/** An AniDB UDP API response outside the small set {@link AniDbUdpClient} handles itself. */
public class AniDbException extends RuntimeException {

  private final int code;

  public AniDbException(int code, String message) {
    super(code + " " + message);
    this.code = code;
  }

  /** True for the response codes AniDB uses for its ban policy — 504/505/555. */
  public boolean isBan() {
    return code == 504 || code == 505 || code == 555;
  }
}
