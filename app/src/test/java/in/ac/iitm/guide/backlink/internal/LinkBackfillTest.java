package in.ac.iitm.guide.backlink.internal;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.shared.persistence.Article;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** DEBT-006: articles written before {@code backlink} existed get their links at the next start. */
@SpringBootTest
class LinkBackfillTest {

    private static final OffsetDateTime MONDAY = OffsetDateTime.parse("2026-09-28T10:00:00+05:30");

    @Autowired
    private LinkBackfill backfill;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
    }

    @Test
    // trace:FR-006
    void articles_with_no_link_rows_get_them_when_the_table_is_empty_at_start_up() {
        written("Hostel Life", "Rooms and mess.");
        written("Arriving", "Then read [[Hostel Life]].");

        backfill.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForList("SELECT target_title FROM article_link", String.class))
                .containsExactly("hostel-life");
    }

    @Test
    // trace:FR-006
    void a_table_that_has_rows_is_left_as_it_is() {
        written("Arriving", "Then read [[Hostel Life]].");
        jdbc.update("INSERT INTO article_link (source_article_id, target_title) SELECT id, 'kept' FROM article");

        backfill.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForList("SELECT target_title FROM article_link", String.class))
                .containsExactly("kept");
    }

    /** Straight to the table, as the importer did before this slice: no event, no links. */
    private void written(String title, String body) {
        transaction.executeWithoutResult(status -> {
            var article = new Article();
            article.setTitle(title);
            article.setSlug(title.toLowerCase().replace(' ', '-'));
            article.setSummary("A summary.");
            article.setBody(body);
            article.setPublishedAt(MONDAY);
            article.setUpdatedAt(MONDAY);
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }
}
