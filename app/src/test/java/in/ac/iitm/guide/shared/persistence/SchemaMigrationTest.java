package in.ac.iitm.guide.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercises the Flyway migration in {@code db/migration} and the eight JPA entity mappings in this
 * package against a real H2 database. The migration is the schema's source of truth, not the entity
 * annotations (docs/ai/testing.md, "Persistence | H2 with the real migrations").
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class SchemaMigrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    // trace:FR-001
    void article_round_trips_through_the_real_migration() {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle("Registering with FRRO");
        article.setSlug("registering-with-frro");
        article.setSummary("How to register with the Foreigners Regional Registration Office.");
        article.setBody("Full text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);

        entityManager.persistAndFlush(article);
        entityManager.clear();

        var found = entityManager.find(Article.class, article.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Registering with FRRO");
        assertThat(found.getSlug()).isEqualTo("registering-with-frro");
        assertThat(found.getPinnedAt()).isNull();
        assertThat(found.getRemovedAt()).isNull();
    }

    @Test
    // trace:FR-009
    void article_pinned_at_is_indexed_so_the_homepage_does_not_scan_every_row() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var indexedColumns = new HashSet<String>();
            try (ResultSet indexInfo = connection.getMetaData().getIndexInfo(null, null, "ARTICLE", false, false)) {
                while (indexInfo.next()) {
                    indexedColumns.add(indexInfo.getString("COLUMN_NAME"));
                }
            }
            assertThat(indexedColumns).contains("PINNED_AT");
        }
    }

    @Test
    // trace:FR-009
    void article_published_at_is_indexed_so_the_recent_list_does_not_sort_every_row() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var indexedColumns = new HashSet<String>();
            try (ResultSet indexInfo = connection.getMetaData().getIndexInfo(null, null, "ARTICLE", false, false)) {
                while (indexInfo.next()) {
                    indexedColumns.add(indexInfo.getString("COLUMN_NAME"));
                }
            }
            assertThat(indexedColumns).contains("PUBLISHED_AT");
        }
    }

    @Test
    // trace:FR-001
    void article_title_is_unique() {
        var now = OffsetDateTime.now();
        var first = new Article();
        first.setTitle("Same title");
        first.setSlug("same-title");
        first.setSummary("s");
        first.setBody("b");
        first.setPublishedAt(now);
        first.setUpdatedAt(now);
        entityManager.persistAndFlush(first);

        var second = new Article();
        second.setTitle("Same title");
        second.setSlug("same-title-other-address");
        second.setSummary("s");
        second.setBody("b");
        second.setPublishedAt(now);
        second.setUpdatedAt(now);

        assertThatThrownBy(() -> entityManager.persistAndFlush(second)).isNotNull();
    }

    @Test
    // trace:FR-001
    void article_slug_is_unique_so_two_titles_cannot_share_an_address() {
        // The two titles differ, so the title constraint does not fire: this is the collision that
        // only the slug constraint sees, and without it the second article would be unreachable.
        var now = OffsetDateTime.now();
        var first = new Article();
        first.setTitle("Fees & Payments");
        first.setSlug("fees-payments");
        first.setSummary("s");
        first.setBody("b");
        first.setPublishedAt(now);
        first.setUpdatedAt(now);
        entityManager.persistAndFlush(first);

        var second = new Article();
        second.setTitle("Fees Payments");
        second.setSlug("fees-payments");
        second.setSummary("s");
        second.setBody("b");
        second.setPublishedAt(now);
        second.setUpdatedAt(now);

        assertThatThrownBy(() -> entityManager.persistAndFlush(second)).isNotNull();
    }

    @Test
    // trace:FR-001
    void article_slug_is_required_by_the_schema_itself() {
        // Inserted with SQL, not through the entity: @Column(nullable = false) would make Hibernate
        // refuse a null slug before any statement ran, and this test would stay green if V5 lost its
        // NOT NULL.
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO article (id, title, summary, body, published_at, updated_at)"
                                + " VALUES (?, 'No address', 's', 'b', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        UUID.randomUUID()))
                .hasMessageContaining("SLUG");
    }

    @Test
    // trace:FR-010
    void submission_round_trips_through_the_real_migration() {
        var now = OffsetDateTime.now();
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-K7M2-QX9P-4TVB");
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Getting a SIM card");
        submission.setSummary("Where to buy one near campus.");
        submission.setBody("Full text.");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(now);

        entityManager.persistAndFlush(submission);
        entityManager.clear();

        var found = entityManager.find(Submission.class, submission.getId());
        assertThat(found).isNotNull();
        assertThat(found.getType()).isEqualTo(SubmissionType.NEW_ARTICLE);
        assertThat(found.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(found.getTargetArticleId()).isNull();
        assertThat(found.getDecidedAt()).isNull();
    }

    @Test
    // trace:FR-020
    void revision_round_trips_through_the_real_migration() {
        var article = persistedArticle();
        var revision = new Revision();
        revision.setArticleId(article.getId());
        revision.setTitle(article.getTitle());
        revision.setSummary(article.getSummary());
        revision.setBody(article.getBody());
        revision.setRetainedAt(OffsetDateTime.now());

        entityManager.persistAndFlush(revision);
        entityManager.clear();

        var found = entityManager.find(Revision.class, revision.getId());
        assertThat(found).isNotNull();
        assertThat(found.getArticleId()).isEqualTo(article.getId());
    }

    @Test
    // trace:FR-008
    void tag_name_is_unique() {
        entityManager.persistAndFlush(newTag("frro"));

        assertThatThrownBy(() -> entityManager.persistAndFlush(newTag("frro"))).isNotNull();
    }

    @Test
    // trace:FR-008
    void article_carries_its_tags_through_the_join_table() {
        var article = persistedArticle();
        var tag = entityManager.persistAndFlush(newTag("visa"));
        article.getTags().add(tag);

        entityManager.persistAndFlush(article);
        entityManager.clear();

        var found = entityManager.find(Article.class, article.getId());
        assertThat(found.getTags()).extracting(Tag::getName).containsExactly("visa");
    }

    @Test
    // trace:FR-010
    void submission_carries_its_suggested_tags_through_the_join_table() {
        var tag = entityManager.persistAndFlush(newTag("hostel"));
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-AAAA-BBBB-CCCC");
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Hostel move-in");
        submission.setSummary("s");
        submission.setBody("b");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission.setTags(Set.of(tag));

        entityManager.persistAndFlush(submission);
        entityManager.clear();

        var found = entityManager.find(Submission.class, submission.getId());
        assertThat(found.getTags()).extracting(Tag::getName).containsExactly("hostel");
    }

    @Test
    // trace:FR-001
    void media_asset_round_trips_through_the_real_migration() {
        var article = persistedArticle();
        var media = new MediaAsset();
        media.setArticleId(article.getId());
        media.setStoredName(UUID.randomUUID() + ".jpg");
        media.setOriginalName("passport-photo.jpg");
        media.setContentType("image/jpeg");
        media.setSizeBytes(1024L);
        media.setUploadedAt(OffsetDateTime.now());

        entityManager.persistAndFlush(media);
        entityManager.clear();

        var found = entityManager.find(MediaAsset.class, media.getId());
        assertThat(found).isNotNull();
        assertThat(found.getSubmissionId()).isNull();
    }

    @Test
    // trace:FR-021
    void report_round_trips_through_the_real_migration() {
        var article = persistedArticle();
        var report = new Report();
        report.setArticleId(article.getId());
        report.setMessage("The fee amount looks outdated.");
        report.setReportedAt(OffsetDateTime.now());

        entityManager.persistAndFlush(report);
        entityManager.clear();

        var found = entityManager.find(Report.class, report.getId());
        assertThat(found).isNotNull();
        assertThat(found.getClosedAt()).isNull();
    }

    @Test
    // trace:FR-004
    void article_link_round_trips_through_the_real_migration() {
        var article = persistedArticle();
        var link = new ArticleLink();
        link.setSourceArticleId(article.getId());
        link.setTargetTitle("A page that does not exist yet");

        entityManager.persistAndFlush(link);
        entityManager.clear();

        var found = entityManager.find(
                ArticleLink.class, new ArticleLinkId(article.getId(), "A page that does not exist yet"));
        assertThat(found).isNotNull();
        assertThat(found.getTargetArticleId()).isNull();
    }

    private Article persistedArticle() {
        var now = OffsetDateTime.now();
        var article = new Article();
        var unique = UUID.randomUUID();
        article.setTitle("Article " + unique);
        article.setSlug("article-" + unique);
        article.setSummary("s");
        article.setBody("b");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        return entityManager.persistAndFlush(article);
    }

    private Tag newTag(String name) {
        var tag = new Tag();
        tag.setName(name);
        return tag;
    }
}
