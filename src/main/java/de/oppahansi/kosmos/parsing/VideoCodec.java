package de.oppahansi.kosmos.parsing;

import java.util.regex.Pattern;

/**
 * Recognized video codecs. Canonical values and aliases are ported from guessit's video_codec
 * property dictionary (guessit-io/guessit, LGPL-3.0).
 */
public enum VideoCodec implements ReleaseToken {
  H265("H.265", "h-?265", "x-?265", "hevc"),
  H264("H.264", "h-?264", "x-?264", "avc(?:hd)?", "mpeg-?4avc"),
  H263("H.263", "h-?263", "x-?263"),
  VP9("VP9", "vp9"),
  VP8("VP8", "vp8", "vp80"),
  VP7("VP7", "vp7"),
  VC1("VC-1", "vc-?1"),
  XVID("Xvid", "xvid"),
  DIVX("DivX", "dvdivx", "divx"),
  MPEG2("MPEG-2", "mpe?g-?2", "h-?262", "x-?262"),
  REAL_VIDEO("RealVideo", "rv\\d{2}");

  private final String canonicalName;
  private final Pattern pattern;

  VideoCodec(String canonicalName, String... aliases) {
    this.canonicalName = canonicalName;
    this.pattern =
        Pattern.compile("\\b(?:" + String.join("|", aliases) + ")\\b", Pattern.CASE_INSENSITIVE);
  }

  @Override
  public String canonicalName() {
    return canonicalName;
  }

  @Override
  public Pattern pattern() {
    return pattern;
  }
}
