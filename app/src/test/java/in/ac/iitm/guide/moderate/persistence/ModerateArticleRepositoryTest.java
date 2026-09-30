package in.ac.iitm.guide.moderate.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.guide.shared.persistence.Article;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The article an approved edit changes is locked for the approval, so two moderators approving two
 * edits of one article are served one after the other: the second reads the text the first wrote
 * and retains that as its revision. Unlocked, both would retain the same old text and the first
 * edit's text would be kept nowhere (FR-020; review of FEAT-006, 28 Sep).
 */
@SpringBootTest
class ModerateArticleRepositoryTest {

    @Autowired
    private ModerateArticleRepository articles;

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
    // trace:FR-020
    void an_article_read_for_an_edit_approval_cannot_be_read_so_by_a_second_approval_until_the_first_ends()
            throws Exception {
        var id = transaction.execute(status -> {
            var article = new Article();
            article.setTitle("Hostel Life");
            article.setSlug("hostel-life");
            article.setSummary("A summary.");
            article.setBody("The original text.");
            article.setPublishedAt(OffsetDateTime.now());
            article.setUpdatedAt(OffsetDateTime.now());
            article.setTags(Set.of());
            entityManager.persist(article);
            return article.getId();
        });
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);

        var first = CompletableFuture.runAsync(() -> transaction.executeWithoutResult(status -> {
            articles.findWithTagsByIdAndRemovedAtIsNull(id).orElseThrow();
            locked.countDown();
            await(release);
        }));
        try {
            assertThat(locked.await(10, TimeUnit.SECONDS))
                    .as("the first approval read the article")
                    .isTrue();

            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                        waitForLocksBriefly();
                        articles.findWithTagsByIdAndRemovedAtIsNull(id);
                    }))
                    .isInstanceOf(PessimisticLockingFailureException.class);
        } finally {
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
        }
    }

    /** Both databases wait for a lock far longer than a test should; this transaction gives up quickly. */
    private void waitForLocksBriefly() {
        var postgres = jdbc.execute((Connection connection) ->
                connection.getMetaData().getDatabaseProductName().contains("PostgreSQL"));
        var sql = Boolean.TRUE.equals(postgres) ? "SET LOCAL lock_timeout = '200ms'" : "SET LOCK_TIMEOUT 200";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
