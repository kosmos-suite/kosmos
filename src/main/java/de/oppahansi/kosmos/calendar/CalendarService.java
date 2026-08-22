package de.oppahansi.kosmos.calendar;

import de.oppahansi.kosmos.media.AnimeEpisode;
import de.oppahansi.kosmos.media.Episode;
import de.oppahansi.kosmos.media.Movie;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds calendar rows on demand from data that already exists elsewhere (movie release dates,
 * episode air dates) rather than a separate materialized "calendar" table — the three source
 * queries below are cheap and this is never a hot path.
 */
@ApplicationScoped
public class CalendarService {

  public List<CalendarEntry> between(LocalDate from, LocalDate to, boolean monitoredOnly) {
    List<CalendarEntry> entries = new ArrayList<>();
    entries.addAll(movieEntries(from, to, monitoredOnly));
    entries.addAll(episodeEntries(from, to, monitoredOnly));
    entries.addAll(animeEpisodeEntries(from, to, monitoredOnly));
    entries.sort(Comparator.comparing(CalendarEntry::date));
    return entries;
  }

  private List<CalendarEntry> movieEntries(LocalDate from, LocalDate to, boolean monitoredOnly) {
    String query =
        "releaseDate between ?1 and ?2" + (monitoredOnly ? " and qualityProfile is not null" : "");
    List<Movie> movies = Movie.list(query, from, to);
    return movies.stream()
        .map(
            movie ->
                new CalendarEntry(
                    movie.mediaItemId,
                    "movie",
                    movie.mediaItem.title,
                    movie.releaseDate,
                    movie.qualityProfile != null,
                    null,
                    null,
                    movie.posterPath))
        .toList();
  }

  private List<CalendarEntry> episodeEntries(LocalDate from, LocalDate to, boolean monitoredOnly) {
    String query =
        "airDate between ?1 and ?2"
            + (monitoredOnly ? " and season.show.qualityProfile is not null" : "");
    List<Episode> episodes = Episode.list(query, from, to);
    return episodes.stream()
        .map(
            episode ->
                new CalendarEntry(
                    episode.mediaItemId,
                    "episode",
                    episode.season.show.mediaItem.title + " - " + episode.mediaItem.title,
                    episode.airDate,
                    episode.season.show.qualityProfile != null,
                    episode.season.seasonNumber,
                    episode.episodeNumber,
                    episode.stillPath != null ? episode.stillPath : episode.season.show.posterPath))
        .toList();
  }

  private List<CalendarEntry> animeEpisodeEntries(
      LocalDate from, LocalDate to, boolean monitoredOnly) {
    String query =
        "airDate between ?1 and ?2"
            + (monitoredOnly ? " and season.anime.qualityProfile is not null" : "");
    List<AnimeEpisode> episodes = AnimeEpisode.list(query, from, to);
    return episodes.stream()
        .map(
            episode ->
                new CalendarEntry(
                    episode.mediaItemId,
                    "anime_episode",
                    episode.season.anime.mediaItem.title + " - " + episode.mediaItem.title,
                    episode.airDate,
                    episode.season.anime.qualityProfile != null,
                    episode.season.seasonNumber,
                    episode.episodeNumber,
                    episode.stillPath != null
                        ? episode.stillPath
                        : episode.season.anime.posterPath))
        .toList();
  }
}
