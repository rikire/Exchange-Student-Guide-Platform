package in.ac.iitm.guide.media;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DEBT-016, walkthrough fix 1.9: the files of a rejected submission go once it has been rejected
 * longer than {@code guide.media.rejected-kept-for}. The tests' configuration keeps them 2 days rather
 * than the application's 7, which is also 1.10's test of the setting at another value.
 */
@SpringBootTest
class RejectedMediaSweepTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-01T12:00:00Z");

    @Autowired
    private MediaAssets media;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    private int numbers;

    @AfterEach
    void clearTheDatabaseAndTheRoot() throws IOException {
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        MediaTestFiles.empty(MediaTestFiles.ROOT);
    }

    @Test
    // trace:NFR-001
    void a_file_rejected_longer_ago_than_the_setting_is_removed_with_its_row() {
        var item = attach(submission(SubmissionStatus.REJECTED, NOW.minusDays(2).minusMinutes(1)));
        var file = stored(item);

        media.sweepRejected(NOW);

        assertThat(rows(item)).isZero();
        assertThat(file).doesNotExist();
    }

    @Test
    // trace:NFR-001
    void a_file_rejected_more_recently_than_the_setting_is_kept() {
        var item = attach(submission(SubmissionStatus.REJECTED, NOW.minusDays(2).plusMinutes(1)));

        media.sweepRejected(NOW);

        assertThat(rows(item)).isOne();
        assertThat(stored(item)).exists();
    }

    @Test
    // trace:NFR-001
    void a_file_waiting_for_a_decision_is_kept_however_old() {
        var item = attach(submission(SubmissionStatus.PENDING, null));

        media.sweepRejected(NOW.plusYears(1));

        assertThat(rows(item)).isOne();
        assertThat(stored(item)).exists();
    }

    @Test
    // trace:NFR-001
    void a_published_articles_file_is_never_removed() {
        var submission = submission(SubmissionStatus.APPROVED, NOW.minusYears(1));
        var item = attach(submission);
        media.moveToArticle(submission, article());

        media.sweepRejected(NOW.plusYears(1));

        assertThat(rows(item)).isOne();
        assertThat(stored(item)).exists();
    }

    @Test
    // trace:NFR-001
    void the_removed_files_no_longer_count_towards_the_volume() {
        attach(submission(SubmissionStatus.REJECTED, NOW.minusDays(30)));

        media.sweepRejected(NOW);

        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(size_bytes), 0) FROM media_asset", Long.class))
                .isZero();
    }

    private MediaItem attach(UUID submission) {
        var bytes = MediaTestFiles.pdf(2_000);
        return media.attach(submission, new Upload("form.pdf", bytes.length, new ByteArrayResource(bytes)));
    }

    private int rows(MediaItem item) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM media_asset WHERE id = ?", Integer.class, item.id());
    }

    private Path stored(MediaItem item) {
        var name = jdbc.queryForObject("SELECT stored_name FROM media_asset WHERE id = ?", String.class, item.id());
        var path = MediaTestFiles.ROOT.resolve(name);
        assertThat(Files.exists(path)).as("the file was written").isTrue();
        return path;
    }

    private UUID submission(SubmissionStatus status, OffsetDateTime decidedAt) {
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-SWEP-0000-%04d".formatted(++numbers));
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Getting a SIM card " + numbers);
        submission.setSummary("Where to buy one.");
        submission.setBody("Take your passport.");
        submission.setStatus(status);
        submission.setSubmittedAt(NOW.minusYears(2));
        submission.setDecidedAt(decidedAt);
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(tx -> entityManager.persist(submission));
        return submission.getId();
    }

    private UUID article() {
        var article = new Article();
        article.setTitle("Registering with FRRO");
        article.setSlug("registering-with-frro");
        article.setSummary("A summary.");
        article.setBody("Text.");
        article.setPublishedAt(NOW);
        article.setUpdatedAt(NOW);
        article.setTags(new HashSet<>());
        transaction.executeWithoutResult(tx -> entityManager.persist(article));
        return article.getId();
    }
}
