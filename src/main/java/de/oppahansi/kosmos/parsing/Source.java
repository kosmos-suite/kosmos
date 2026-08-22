package de.oppahansi.kosmos.parsing;

import java.util.regex.Pattern;

/**
 * Recognized release sources. Canonical values and aliases are ported from guessit's source
 * property dictionary (guessit-io/guessit, LGPL-3.0), trimmed to what's relevant for movies.
 */
enum Source implements ReleaseToken {
  ULTRA_HD_BLU_RAY("Ultra HD Blu-ray", "ultra-?blu-?ray", "blu-?ray-?ultra"),
  BLU_RAY("Blu-ray", "blu-?ray", "bd", "bd5", "bd9", "bd25", "bd50", "bdrip", "brrip"),
  HD_DVD("HD-DVD", "hd-?dvd"),
  WEB("Web", "web-?dl", "webrip", "webcap", "web-?uhd", "dl-?web"),
  UHDTV("Ultra HDTV", "uhd-?tv", "uhd"),
  HDTV("HDTV", "hd-?tv", "tv-?hd"),
  DVD("DVD", "dvd", "dvdrip", "video-?ts", "dvd-?9", "dvd-?5", "dvdr", "dvd-?scr(?:eener)?"),
  SDTV("TV", "sd-?tv", "tv", "tvrip"),
  TELESYNC("Telesync", "hd-?telesync", "hd-?ts", "telesync", "ts"),
  TELECINE("Telecine", "hd-?telecine", "hd-?tc", "telecine", "tc"),
  CAM("Camera", "hd-?cam", "cam"),
  WORKPRINT("Workprint", "workprint", "wp"),
  SATELLITE("Satellite", "dsr", "dth", "sat"),
  VOD("Video on Demand", "vod"),
  PPV("Pay-per-view", "ppv"),
  LASERDISC("Laserdisc", "laserdisc", "ld"),
  VHS("VHS", "vhs");

  private final String canonicalName;
  private final Pattern pattern;

  Source(String canonicalName, String... aliases) {
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
