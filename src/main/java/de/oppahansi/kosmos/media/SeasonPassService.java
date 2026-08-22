package de.oppahansi.kosmos.media;

import de.oppahansi.kosmos.media.dto.SeasonPassEntry;
import de.oppahansi.kosmos.media.dto.SeasonPassSeason;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the per-show/anime season-by-season completeness view (have vs. total episode count) —
 * reuses the exact status computation {@code ShowResource}/{@code AnimeResource} already use for
 * their own detail pages, just aggregated to a have/total pair per season instead of a per-episode
 * status.
 */
@ApplicationScoped
public class SeasonPassService {

  @Inject ShowService showService;
  @Inject AnimeService animeService;

  public List<SeasonPassEntry> build() {
    List<SeasonPassEntry> entries = new ArrayList<>();
    for (Show show : showService.listAll()) {
      entries.add(showEntry(show));
    }
    for (Anime anime : animeService.listAll()) {
      entries.add(animeEntry(anime));
    }
    entries.sort(Comparator.comparing(SeasonPassEntry::title, String.CASE_INSENSITIVE_ORDER));
    return entries;
  }

  private SeasonPassEntry showEntry(Show show) {
    List<Season> seasons = showService.seasonsFor(show.mediaItemId);
    List<SeasonPassSeason> result = new ArrayList<>();
    for (Season season : seasons) {
      List<Episode> episodes = showService.episodesFor(season.id);
      Map<UUID, String> status =
          MediaItemStatus.forMediaItems(episodes.stream().map(e -> e.mediaItemId).toList());
      long have =
          episodes.stream().filter(e -> "AVAILABLE".equals(status.get(e.mediaItemId))).count();
      result.add(new SeasonPassSeason(season.seasonNumber, (int) have, episodes.size()));
    }
    return new SeasonPassEntry(
        show.mediaItemId, show.mediaItem.title, show.posterPath, "show", result);
  }

  private SeasonPassEntry animeEntry(Anime anime) {
    List<AnimeSeason> seasons = animeService.seasonsFor(anime.mediaItemId);
    List<SeasonPassSeason> result = new ArrayList<>();
    for (AnimeSeason season : seasons) {
      List<AnimeEpisode> episodes = animeService.episodesFor(season.id);
      Map<UUID, String> status =
          MediaItemStatus.forMediaItems(episodes.stream().map(e -> e.mediaItemId).toList());
      long have =
          episodes.stream().filter(e -> "AVAILABLE".equals(status.get(e.mediaItemId))).count();
      result.add(new SeasonPassSeason(season.seasonNumber, (int) have, episodes.size()));
    }
    return new SeasonPassEntry(
        anime.mediaItemId, anime.mediaItem.title, anime.posterPath, "anime", result);
  }
}
