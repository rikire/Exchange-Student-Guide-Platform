package in.ac.iitm.guide.taxonomy;

import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A tag as a page shows it: its name, and the page listing the articles that carry it. Every slice
 * that shows tags builds them here, so a chip leads to the same address everywhere (FEAT-008).
 *
 * <p>The address is the name by the article address rule ({@link ArticleAddress}): tags are free-form
 * (ADR-0005), and a {@code /}, {@code %}, {@code ;} or {@code \} could not travel in a path segment
 * that Tomcat and Spring Security accept. {@code visa/frro} and {@code visa frro} therefore share one
 * page, and a tag of only symbols has none.
 *
 * @param path {@code /tags/…}, or null when the name has no letters or digits
 */
// trace:FR-008
public record TagLink(String name, String path) {

    public static TagLink of(String name) {
        return new TagLink(name, addressOf(name).map(TagLink::pathOf).orElse(null));
    }

    /** @return the tags sorted by name, each with its page */
    public static List<TagLink> of(Collection<String> names) {
        return names.stream().sorted().map(TagLink::of).toList();
    }

    /** @return the address a tag's page is found under; empty for a name of only symbols */
    public static Optional<String> addressOf(String name) {
        return ArticleAddress.slugOf(name);
    }

    private static String pathOf(String address) {
        // As ArticleAddress.pathOf: an address holds only letters, marks, digits and hyphens, so form
        // encoding never meets the space it would turn into "+".
        return "/tags/" + URLEncoder.encode(address, StandardCharsets.UTF_8);
    }
}
