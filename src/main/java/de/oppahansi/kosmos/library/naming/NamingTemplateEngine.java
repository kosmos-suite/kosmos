package de.oppahansi.kosmos.library.naming;

import de.oppahansi.kosmos.library.LibraryPathNaming;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a {@code {Token}} template against a {@link NamingContext} into a single sanitized path
 * segment (folder or file name, not a whole path — callers join segments themselves, same as before
 * this existed). Pure and stateless on purpose: no DB/CDI dependency, so it's trivially
 * unit-testable and reusable for a live preview in the settings UI without a round trip.
 *
 * <p>Supported tokens: {@code {Title}}, {@code {Year}}, {@code {EpisodeTitle}}, {@code {Season}},
 * {@code {Episode}}, {@code {Absolute}}. A numeric token accepts a zero-padding suffix — {@code
 * {Season:00}} pads to the width of the zeros given, e.g. {@code 3} -> {@code "03"}. An unknown
 * token, or a token whose backing field is null for this context (e.g. {@code {Season}} on a
 * movie), resolves to an empty string rather than failing the whole render — a template written for
 * the wrong content type degrades to a slightly odd name, not a broken import.
 */
public final class NamingTemplateEngine {

  private static final Pattern TOKEN = Pattern.compile("\\{(\\w+)(?::(0+))?}");

  public String render(String template, NamingContext context) {
    Matcher matcher = TOKEN.matcher(template);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      String value = resolve(matcher.group(1), matcher.group(2), context);
      matcher.appendReplacement(out, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(out);
    return LibraryPathNaming.sanitize(out.toString());
  }

  private String resolve(String token, String padding, NamingContext ctx) {
    return switch (token) {
      case "Title" -> ctx.title() != null ? ctx.title() : "";
      case "EpisodeTitle" -> ctx.episodeTitle() != null ? ctx.episodeTitle() : "";
      case "Year" -> ctx.year() != null ? String.valueOf(ctx.year()) : "";
      case "Season" -> pad(ctx.seasonNumber(), padding);
      case "Episode" -> pad(ctx.episodeNumber(), padding);
      case "Absolute" -> pad(ctx.absoluteEpisodeNumber(), padding);
      default -> "";
    };
  }

  private String pad(Integer number, String padding) {
    if (number == null) {
      return "";
    }
    int width = padding != null ? padding.length() : 1;
    return String.format("%0" + width + "d", number);
  }
}
