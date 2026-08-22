package de.oppahansi.kosmos.library.naming;

/**
 * Every field a naming template might reference, for one file being named — unused fields for a
 * given content type (e.g. {@code seasonNumber} for a movie) are simply left null, and any token
 * referencing them resolves to an empty string. {@code title} is always the top-level owning title
 * (the movie's own title, or the show's/anime's — never the episode's); {@code episodeTitle} is the
 * one field that's specifically the episode's own.
 */
public record NamingContext(
    String title,
    Integer year,
    String episodeTitle,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer absoluteEpisodeNumber) {

  public static NamingContext forMovie(String title, Integer year) {
    return new NamingContext(title, year, null, null, null, null);
  }

  public static NamingContext forShow(String title, Integer year) {
    return new NamingContext(title, year, null, null, null, null);
  }

  public static NamingContext forEpisode(
      String seriesTitle, Integer year, String episodeTitle, int seasonNumber, int episodeNumber) {
    return new NamingContext(seriesTitle, year, episodeTitle, seasonNumber, episodeNumber, null);
  }

  public static NamingContext forAnimeEpisode(
      String animeTitle, Integer year, String episodeTitle, Integer absoluteEpisodeNumber) {
    return new NamingContext(animeTitle, year, episodeTitle, null, null, absoluteEpisodeNumber);
  }
}
