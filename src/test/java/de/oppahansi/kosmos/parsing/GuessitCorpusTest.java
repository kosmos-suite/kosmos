package de.oppahansi.kosmos.parsing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.oppahansi.kosmos.parsing.dto.ParsedRelease;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Runs {@link ReleaseParser} against guessit's own real-world test fixtures (guessit-io/guessit,
 * LGPL-3.0 — {@code guessit/test/movies.yml} and {@code episodes.yml}, converted to {@code
 * resources/guessit/*.json}: filenames only, guessit's YAML expected-output schema mapped onto
 * {@link ParsedRelease}'s corresponding fields, with any expectation not actually derivable from
 * the bare filename dropped at conversion time — guessit sometimes relies on folder-path context (a
 * year or season number only present in the parent directory) that {@code ReleaseParser} never
 * receives, since Kosmos only ever parses a flat release title, never a path).
 *
 * <p>This is a regression guard, not a 100%-equivalence assertion — see the roadmap's own framing
 * for why. A large chunk of the corpus's remaining disagreements are legitimately out of scope for
 * a regex-based parser: titles guessit only resolves via some external scene-release database
 * (single-token obfuscated names like {@code "wthd-cab"} → "Charlie And Boots" — unrecoverable from
 * the string alone by any general approach), international-language season/episode markers (Spanish
 * "Temporada"/"Capitulo", Russian Cyrillic "Сезон"/"серия", German "Staffel"/"Folge" — real future
 * work, not modeled today), a parenthesized alternate title guessit strips from the clean title
 * ({@code "Le Prestige (The Prestige)"} → "Le Prestige" — too fuzzy a heuristic to add safely, real
 * titles legitimately contain parenthetical text), and "Part N" sequel-number stripping (risks
 * truncating a real title that happens to contain "Part"). Each round of fixing this test drove
 * real ReleaseParser improvements (resolution/source/edition as clean-title cut markers,
 * trailing-punctuation trimming, a parenthesized year preferred over a bare in-title number, Dolby
 * Digital matching a directly-attached channel count like "DD5.1", DVD-R/DVDScr/ TVRip source
 * aliases, tolerant SxxExx separators) — the threshold below is the resulting baseline, not an
 * arbitrary number; a regression drops it, a further improvement can tighten it.
 */
class GuessitCorpusTest {

  private final ReleaseParser parser = new ReleaseParser();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void moviesCorpusStaysWithinTheMismatchBaseline() throws Exception {
    assertWithinBaseline("guessit/guessit-movies.json", false, 163);
  }

  @Test
  void episodesCorpusStaysWithinTheMismatchBaseline() throws Exception {
    assertWithinBaseline("guessit/guessit-episodes.json", true, 747);
  }

  private void assertWithinBaseline(String resource, boolean episode, int maxMismatches)
      throws Exception {
    List<Map<String, Object>> cases = load(resource);
    int mismatches = 0;
    List<String> examples = new ArrayList<>();

    for (Map<String, Object> expected : cases) {
      String input = (String) expected.get("input");
      ParsedRelease actual = parser.parse(input);
      List<String> caseMismatches = new ArrayList<>();

      collect(expected, "cleanTitle", actual.cleanTitle(), caseMismatches);
      collect(expected, "year", actual.year(), caseMismatches);
      collect(expected, "resolution", actual.resolution(), caseMismatches);
      collect(expected, "source", actual.source(), caseMismatches);
      collect(expected, "videoCodec", actual.videoCodec(), caseMismatches);
      collect(expected, "audioCodec", actual.audioCodec(), caseMismatches);
      collect(expected, "releaseGroup", actual.releaseGroup(), caseMismatches);
      collect(expected, "edition", actual.edition(), caseMismatches);
      collect(expected, "proper", actual.proper(), caseMismatches);
      collect(expected, "repack", actual.repack(), caseMismatches);
      if (episode) {
        collect(expected, "seasonNumber", actual.seasonNumber(), caseMismatches);
        collect(expected, "episodeNumber", actual.episodeNumber(), caseMismatches);
      }

      mismatches += caseMismatches.size();
      if (!caseMismatches.isEmpty() && examples.size() < 20) {
        examples.add(input + "  =>  " + String.join(" | ", caseMismatches));
      }
    }

    assertTrue(
        mismatches <= maxMismatches,
        "Regressed: "
            + mismatches
            + " field mismatches against "
            + resource
            + ", baseline is "
            + maxMismatches
            + ". First examples:\n"
            + String.join("\n", examples));
  }

  private void collect(Map<String, Object> expected, String key, Object actual, List<String> out) {
    if (!expected.containsKey(key)) {
      return;
    }
    Object exp = expected.get(key);
    boolean equal =
        (exp instanceof Number && actual instanceof Number)
            ? ((Number) exp).intValue() == ((Number) actual).intValue()
            : exp.equals(actual);
    if (!equal) {
      out.add(key + ": expected=" + exp + " actual=" + actual);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> load(String resource) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
      JsonNode root = objectMapper.readTree(in);
      return objectMapper.convertValue(root, List.class);
    }
  }
}
