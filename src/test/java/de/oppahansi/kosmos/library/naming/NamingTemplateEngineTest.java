package de.oppahansi.kosmos.library.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NamingTemplateEngineTest {

  private final NamingTemplateEngine engine = new NamingTemplateEngine();

  @Test
  void rendersMovieFolder() {
    NamingContext context = NamingContext.forMovie("Belle", 2021);
    assertEquals("Belle (2021)", engine.render("{Title} ({Year})", context));
  }

  @Test
  void padsSeasonAndEpisodeToTheGivenWidth() {
    NamingContext context = NamingContext.forEpisode("Show", 2020, "Pilot", 1, 5);
    assertEquals(
        "Show - S01E05 - Pilot",
        engine.render("{Title} - S{Season:00}E{Episode:00} - {EpisodeTitle}", context));
  }

  @Test
  void padsToAWiderWidthWhenTheTemplateAsksForIt() {
    NamingContext context = NamingContext.forEpisode("Show", 2020, "Ep", 1, 7);
    assertEquals("E007", engine.render("E{Episode:000}", context));
  }

  @Test
  void animeUsesAbsoluteEpisodeNumberAcrossSeasons() {
    NamingContext context =
        NamingContext.forAnimeEpisode("Attack on Titan", 2013, "To You, 2000 Years From Now", 26);
    assertEquals(
        "Attack on Titan - 26 - To You, 2000 Years From Now",
        engine.render("{Title} - {Absolute} - {EpisodeTitle}", context));
  }

  @Test
  void unknownTokenAndMissingFieldBothResolveToEmptyRatherThanFailing() {
    NamingContext context = NamingContext.forMovie("Belle", 2021);
    assertEquals("Belle -", engine.render("{Title} - {Season:00}{NotAToken}", context));
  }

  @Test
  void sanitizesIllegalFilenameCharactersInResolvedValues() {
    NamingContext context = NamingContext.forMovie("Ocean's Eleven: Redux", 2001);
    assertEquals("Ocean's Eleven Redux (2001)", engine.render("{Title} ({Year})", context));
  }
}
