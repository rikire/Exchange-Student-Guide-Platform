package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.shared.persistence.Article;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The index is a directory on disk kept between starts (ADR-0004), and every write goes through JPA
 * and is indexed on commit. An empty index — the first start, or a lost {@code guide-index} volume —
 * is filled from the {@code article} table here; one that has documents is left as it is (decided by
 * the human, 28 Sep). First among the runners so that search is complete as early as possible.
 */
// trace:FR-007
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SearchIndexBuilder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexBuilder.class);

    private final EntityManagerFactory entityManagerFactory;

    SearchIndexBuilder(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
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
        Search.mapping(entityManagerFactory).scope(Article.class).massIndexer().startAndWait();
    }
}
