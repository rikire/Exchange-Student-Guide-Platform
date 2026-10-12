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
 * and is indexed on commit. At every start it is dropped and built again from the {@code article}
 * table (decided by the human, 5 Oct, reversing 28 Sep): an index an older version wrote may hold
 * another mapping, which Lucene refuses to write into, and the demo rehearsal's approval answered
 * {@code 500} for it. The guide's few hundred articles make that cheap.
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
        log.info("Building the search index from the article table");
        try {
            // Dropping the schema, not only purging documents: Lucene keeps a field's settings in
            // its segments, and those are what a changed mapping conflicts with.
            Search.mapping(entityManagerFactory)
                    .scope(Article.class)
                    .massIndexer()
                    .dropAndCreateSchemaOnStart(true)
                    .startAndWait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while building the search index", e);
        }
    }
}
