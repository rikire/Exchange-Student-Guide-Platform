package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.shared.persistence.Article;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * The index is a directory on disk kept between starts (ADR-0004), and every write goes through JPA
 * and is indexed on commit. An empty index — the first start, or a lost {@code guide-index} volume —
 * is filled from the {@code article} table here; one that has documents is left as it is (decided by
 * the human, 28 Sep).
 *
 * <p>It runs as the context starts: after anything that writes articles at start-up, such as the
 * seed, and before the web server, so the first request already searches every article
 * (walkthrough fix 1.6).
 */
// trace:FR-007
@Component
class SearchIndexBuilder implements SmartLifecycle {

    /** After start-up importers, which take earlier phases; before the web server, at {@code - 1024}. */
    static final int PHASE = WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 2048;

    private static final Logger log = LoggerFactory.getLogger(SearchIndexBuilder.class);

    private final EntityManagerFactory entityManagerFactory;
    private volatile boolean running;

    SearchIndexBuilder(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void start() {
        build();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    private void build() {
        long documents;
        try (var entityManager = entityManagerFactory.createEntityManager()) {
            documents = Search.session(entityManager)
                    .search(Article.class)
                    .where(f -> f.matchAll())
                    .fetchTotalHitCount();
        }
        if (documents > 0) {
            log.info("Search index holds {} articles; not rebuilt", documents);
            return;
        }
        log.info("Search index is empty; building it from the article table");
        try {
            Search.mapping(entityManagerFactory)
                    .scope(Article.class)
                    .massIndexer()
                    .startAndWait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while building the search index", e);
        }
    }
}
