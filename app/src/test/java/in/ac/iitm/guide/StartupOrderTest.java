package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.shared.persistence.Article;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * F-1 (walkthrough fix 1.6): the guide answers its first request only once the seed is imported and
 * the search index holds it, so a search right after a start from empty volumes sees every article.
 * What the database and the index hold is read at the moment the web server starts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("seed")
@Import(StartupOrderTest.AtWebServerStart.class)
class StartupOrderTest {

    @TestConfiguration
    static class AtWebServerStart {

        long articles = -1;
        long indexed = -1;

        @Bean
        ApplicationListener<ServletWebServerInitializedEvent> countWhenTheServerStarts(
                JdbcTemplate jdbc, EntityManagerFactory entityManagerFactory) {
            return event -> {
                articles = jdbc.queryForObject("SELECT count(*) FROM article", Long.class);
                try (var entityManager = entityManagerFactory.createEntityManager()) {
                    indexed = Search.session(entityManager)
                            .search(Article.class)
                            .where(f -> f.matchAll())
                            .fetchTotalHitCount();
                }
            };
        }
    }

    @Autowired
    private AtWebServerStart atStart;

    @Test
    // trace:FR-007
    // trace:NFR-004
    void the_seed_is_imported_and_indexed_before_the_web_server_starts() {
        assertThat(atStart.articles).as("articles in the database").isGreaterThanOrEqualTo(20);
        assertThat(atStart.indexed).as("articles in the search index").isEqualTo(atStart.articles);
    }
}
