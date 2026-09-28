package in.ac.iitm.guide.backlink.internal;

import in.ac.iitm.guide.backlink.persistence.LinkArticleRepository;
import in.ac.iitm.guide.backlink.persistence.LinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty {@code article_link} from the articles already there — those written before this
 * slice existed (DEBT-006) — once, at start-up, as the search index is filled. A table with any row is
 * left as it is: from then on every write path publishes {@code ArticleTextChanged}.
 */
// trace:FR-006
@Component
class LinkBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LinkBackfill.class);

    private final LinkRepository links;
    private final LinkArticleRepository articles;
    private final LinkIndexer indexer;

    LinkBackfill(LinkRepository links, LinkArticleRepository articles, LinkIndexer indexer) {
        this.links = links;
        this.articles = articles;
        this.indexer = indexer;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (links.count() > 0) {
            return;
        }
        var ids = articles.findAllIds();
        if (ids.isEmpty()) {
            return;
        }
        ids.forEach(indexer::index);
        log.info("Link table was empty; read the links of {} articles", ids.size());
    }
}
