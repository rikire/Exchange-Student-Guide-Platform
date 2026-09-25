package in.ac.iitm.guide;

import static com.vladmihalcea.sql.SQLStatementCountValidator.assertSelectCount;
import static com.vladmihalcea.sql.SQLStatementCountValidator.reset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The N+1 rule (ADR-0010): what a page costs in queries is set by the page, not by how much content
 * exists. Each test measures a small page, then makes
 * the content several times larger and asserts the number of {@code SELECT}s did not move.
 *
 * <p>Equality, not a fixed number, so a query that is legitimately reshaped does not break the test
 * and a query added per row does. The baseline must be above zero, otherwise a counter that is not
 * wired to the database would make every comparison pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PageQueryCountTest.CountingDataSource.class)
class PageQueryCountTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-25T12:00:00+05:30");

    /** Wraps the application's data source so every statement it runs is counted. */
    @TestConfiguration(proxyBeanMethods = false)
    static class CountingDataSource {
        @Bean
        static BeanPostProcessor countStatements() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource dataSource) {
                        return ProxyDataSourceBuilder.create(dataSource)
                                .countQuery()
                                .build();
                    }
                    return bean;
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-009
    void the_landing_page_runs_the_same_queries_whether_it_lists_two_articles_or_thirty() throws Exception {
        // Both lists non-empty, so the tag loading of each is part of the baseline.
        seedLanding(1, 1);
        var baseline = selectsFor("/");
        assertThat(baseline).as("SELECTs of the smallest landing page").isPositive();

        clearTheDatabase();
        seedLanding(2, 8);
        reset();
        mockMvc.perform(get("/")).andExpect(status().isOk());
        assertSelectCount(baseline);

        // Far more rows than the first two sizes: a batch size too small for a full page shows here.
        clearTheDatabase();
        seedLanding(15, 15);
        reset();
        mockMvc.perform(get("/")).andExpect(status().isOk());
        assertSelectCount(baseline);
    }

    @Test
    // trace:FR-001
    void an_article_page_runs_the_same_queries_whatever_the_number_of_wiki_links_in_its_text() throws Exception {
        seedArticleLinking(1);
        var baseline = selectsFor("/articles/arrival");
        assertThat(baseline)
                .as("SELECTs of an article with one existing and one missing link")
                .isPositive();

        clearTheDatabase();
        seedArticleLinking(10);
        reset();
        mockMvc.perform(get("/articles/arrival")).andExpect(status().isOk());
        assertSelectCount(baseline);
    }

    private long selectsFor(String path) throws Exception {
        reset();
        mockMvc.perform(get(path)).andExpect(status().isOk());
        return QueryCountHolder.getGrandTotal().getSelect();
    }

    private void seedLanding(int pinned, int recent) {
        IntStream.range(0, pinned)
                .forEach(i -> publish("Pinned " + i, "Text.", NOW.minusDays(50 + i), NOW.minusDays(1 + i)));
        IntStream.range(0, recent).forEach(i -> publish("Recent " + i, "Text.", NOW.minusDays(i), null));
    }

    /** "Arrival" links to {@code count} published articles and to {@code count} that do not exist. */
    private void seedArticleLinking(int count) {
        var body = new StringBuilder("Read");
        for (var i = 0; i < count; i++) {
            publish("Target " + i, "Text.", NOW.minusDays(1), null);
            body.append(" [[Target ")
                    .append(i)
                    .append("]] [[Missing ")
                    .append(i)
                    .append("]]");
        }
        publish("Arrival", body.toString(), NOW.minusDays(1), null);
    }

    /** Every article carries two tags of its own, so a tag lookup per article would show. */
    private void publish(String title, String body, OffsetDateTime publishedAt, OffsetDateTime pinnedAt) {
        var tags = new HashSet<Tag>();
        for (var suffix : new String[] {" one", " two"}) {
            var tag = new Tag();
            tag.setName(title.toLowerCase() + suffix);
            tags.add(tag);
        }
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody(body);
        article.setPublishedAt(publishedAt);
        article.setUpdatedAt(publishedAt);
        article.setPinnedAt(pinnedAt);
        article.setTags(Set.copyOf(tags));
        transaction.executeWithoutResult(status -> {
            tags.forEach(entityManager::persist);
            entityManager.persist(article);
        });
    }
}
