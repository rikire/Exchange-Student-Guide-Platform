package in.ac.iitm.guide.backlink.internal;

import in.ac.iitm.guide.backlink.ArticleTextChanged;
import in.ac.iitm.guide.backlink.persistence.LinkArticleRepository;
import in.ac.iitm.guide.backlink.persistence.LinkRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.ArticleLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-reads one article's links into {@code article_link} (ADR-0016): its own links, replaced; and the
 * links other articles wrote to its address, pointed at it. Runs in the transaction of whoever
 * published {@link ArticleTextChanged}.
 */
// trace:FR-006
@Component
class LinkIndexer {

    private final LinkRepository links;
    private final LinkArticleRepository articles;
    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    LinkIndexer(LinkRepository links, LinkArticleRepository articles) {
        this.links = links;
        this.articles = articles;
    }

    @EventListener
    @Transactional
    void on(ArticleTextChanged changed) {
        index(changed.articleId());
    }

    @Transactional
    void index(UUID articleId) {
        var article = articles.findById(articleId)
                .orElseThrow(() -> new IllegalStateException("No article with id " + articleId));
        links.deleteFromSource(articleId);
        saveLinksOf(article);
        links.pointAt(articleId, article.getSlug());
        links.unpointOthers(articleId, article.getSlug());
    }

    /** One row per address linked, the article's own address left out: a page is not its own backlink. */
    private void saveLinksOf(Article article) {
        var addresses = new LinkedHashSet<String>();
        for (var title : renderer.linkedTitles(article.getBody())) {
            ArticleAddress.slugOf(title)
                    .filter(address -> !address.equals(article.getSlug()))
                    .ifPresent(addresses::add);
        }
        if (addresses.isEmpty()) {
            return;
        }
        var standing = articles.findBySlugIn(addresses).stream()
                .collect(Collectors.toMap(Article::getSlug, Function.identity()));
        for (var address : addresses) {
            var link = new ArticleLink();
            link.setSourceArticleId(article.getId());
            link.setTargetTitle(address);
            var target = standing.get(address);
            link.setTargetArticleId(target == null ? null : target.getId());
            links.save(link);
        }
    }
}
