package in.ac.iitm.guide.moderate.internal;

import in.ac.iitm.guide.moderate.persistence.ModerateArticleRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-026: the moderator takes a published article down. The row stays, with {@code removed_at} set;
 * every public read already leaves such an article out, and the search index drops it when the change
 * is committed. There is no way back through the application, which is why removal asks first.
 */
// trace:FR-026
@Service
public class ArticleRemoval {

    private static final Logger log = LoggerFactory.getLogger(ArticleRemoval.class);

    private final ModerateArticleRepository articles;

    ArticleRemoval(ModerateArticleRepository articles) {
        this.articles = articles;
    }

    /** What the confirmation shows: the title, and the page Cancel goes back to. */
    public record Candidate(String title, String path) {}

    /** @throws ArticleNotLiveException if no published article is at the address */
    @Transactional(readOnly = true)
    public Candidate candidate(String address) {
        var article = live(address);
        return new Candidate(article.getTitle(), ArticleAddress.pathOf(article.getSlug()));
    }

    /**
     * @return the removed article's title
     * @throws ArticleNotLiveException if no published article is at the address
     */
    @Transactional
    public String remove(String address) {
        var article = live(address);
        article.setRemovedAt(OffsetDateTime.now());
        articles.save(article);
        log.info("Removed article at address {}", article.getSlug());
        return article.getTitle();
    }

    private Article live(String address) {
        return ArticleAddress.slugOf(address)
                .flatMap(articles::findBySlugAndRemovedAtIsNull)
                .orElseThrow(() -> new ArticleNotLiveException(address));
    }
}
