package in.ac.iitm.guide.contribute.internal;

import in.ac.iitm.guide.contribute.persistence.ContributeArticleRepository;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The editor's preview (FR-027, ADR-0013): a body rendered by the same renderer and the same
 * live-article rule as the article page, so the contributor sees what would be published. Anonymous
 * and public, so the text is bounded in size here and in rate by NFR-005's limit on its route.
 */
// trace:FR-027
@Service
public class BodyPreview {

    /** Measured as {@link String#length()}, as the title is; decided with the human on 28 Sep. */
    public static final int LONGEST_BODY = 100_000;

    private final ContributeArticleRepository articles;
    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    BodyPreview(ContributeArticleRepository articles) {
        this.articles = articles;
    }

    /** @throws PreviewTooLongException when the body is longer than {@link #LONGEST_BODY} */
    @Transactional(readOnly = true)
    public String render(String body) {
        if (body.length() > LONGEST_BODY) {
            throw new PreviewTooLongException();
        }
        return renderer.render(body, this::resolve);
    }

    /** Which linked titles are live articles: one query for the whole body. */
    private Map<String, String> resolve(Set<String> titles) {
        var slugOfTitle = new HashMap<String, String>();
        for (var title : titles) {
            ArticleAddress.slugOf(title).ifPresent(slug -> slugOfTitle.put(title, slug));
        }
        if (slugOfTitle.isEmpty()) {
            return Map.of();
        }
        var live = articles.findLiveSlugs(new HashSet<>(slugOfTitle.values()));
        var hrefs = new HashMap<String, String>();
        slugOfTitle.forEach((title, slug) -> {
            if (live.contains(slug)) {
                hrefs.put(title, ArticleAddress.pathOf(slug));
            }
        });
        return hrefs;
    }

    /** A body too long to preview ({@code 413}, ui-routes.md). */
    public static class PreviewTooLongException extends RuntimeException {}
}
