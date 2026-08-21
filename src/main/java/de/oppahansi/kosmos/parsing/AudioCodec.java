package de.oppahansi.kosmos.parsing;

import java.util.regex.Pattern;

/**
 * Recognized audio codecs. Canonical values and aliases are ported from guessit's audio_codec
 * property dictionary (guessit-io/guessit, LGPL-3.0).
 */
enum AudioCodec implements ReleaseToken {
  DTS_X("DTS:X", "dts:?x", "dts-?x"),
  DTS_HD("DTS-HD", "dts-?hd", "dts(?=-?ma)"),
  DTS("DTS", "dts"),
  DOLBY_ATMOS("Dolby Atmos", "atmos", "dolby-?atmos"),
  DOLBY_TRUEHD("Dolby TrueHD", "true-?hd"),
  DOLBY_DIGITAL_PLUS("Dolby Digital Plus", "ddp", "dd\\+", "e-?ac-?3"),
  DOLBY_DIGITAL("Dolby Digital", "dolby(?:digital)?", "dolby-digital", "dd", "ac-?3d?"),
  AAC("AAC", "aac"),
  FLAC("FLAC", "flac"),
  OPUS("Opus", "opus"),
  VORBIS("Vorbis", "vorbis"),
  LPCM("LPCM", "lpcm"),
  PCM("PCM", "pcm"),
  MP3("MP3", "mp3", "lame"),
  MP2("MP2", "mp2");

  private final String canonicalName;
  private final Pattern pattern;

  AudioCodec(String canonicalName, String... aliases) {
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
