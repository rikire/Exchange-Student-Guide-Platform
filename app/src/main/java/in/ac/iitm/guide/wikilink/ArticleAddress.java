package in.ac.iitm.guide.wikilink;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Where an article lives: the address (slug) derived from its title, and the path that serves it
 * (docs/architecture/ui-routes.md, "Article identity in a URL").
 *
 * <p>The address is stored in {@code article.slug}, unique, so a page and a wiki link both find an
 * article with one indexed lookup instead of scanning titles (ADR-0010). Plain Java: the rules are
 * string handling, and {@code wikilink} imports no Spring.
 */
// trace:FR-001
public final class ArticleAddress {

    // Marks (\p{M}) are kept along with letters and digits: the vowel signs of Devanagari and Tamil
    // are combining marks, and dropping them would reduce a word to its consonants.
    private static final Pattern NOT_PART_OF_A_WORD = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");

    private ArticleAddress() {}

    /**
     * @return the address of a title: lower case, composed Unicode, each run of characters that are
     *     neither letters, marks nor digits collapsed to one hyphen, hyphens at the ends dropped;
     *     empty when nothing of the title is left, in which case the title has no address
     */
    public static Optional<String> slugOf(String title) {
        var composed = Normalizer.normalize(title.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        var slug = NOT_PART_OF_A_WORD.matcher(composed).replaceAll("-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? Optional.empty() : Optional.of(slug);
    }

    /**
     * @param slug an address from {@link #slugOf}
     * @return the path that serves that article, non-ASCII characters percent-encoded as UTF-8
     */
    public static String pathOf(String slug) {
        // URLEncoder is written for form values, but a slug holds only letters, marks, digits and
        // hyphens: it never contains the space that form encoding would turn into "+".
        return "/articles/" + URLEncoder.encode(slug, StandardCharsets.UTF_8);
    }
}
