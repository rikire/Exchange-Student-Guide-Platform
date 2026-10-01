package in.ac.iitm.guide.search;

import in.ac.iitm.guide.search.internal.ArticleSearchService;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Walkthrough fix 3.8 (F-14): published articles whose titles are close to an address that answers
 * nothing, for the not-found page's "Did you mean". The one type search publishes; the engine stays
 * in {@code internal}.
 */
// trace:FR-001
@Service
public class SimilarTitles {

    /** As many as a reader scans at a glance; the requirement fixes no number. */
    static final int LIMIT = 5;

    /** A title to offer, and where it is. */
    public record Suggestion(String title, String path) {}

    private final ArticleSearchService search;

    SimilarTitles(ArticleSearchService search) {
        this.search = search;
    }

    /**
     * @param address the article address as it arrived, words joined by hyphens
     * @return up to five live articles with a title word within two edits of a word of the address,
     *     the closest first; empty when none is
     */
    public List<Suggestion> near(String address) {
        return search.titledCloseTo(address.replace('-', ' '), LIMIT).stream()
                .map(article -> new Suggestion(article.getTitle(), ArticleAddress.pathOf(article.getSlug())))
                .toList();
    }
}
