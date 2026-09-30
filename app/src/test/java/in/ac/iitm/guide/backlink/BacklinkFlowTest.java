package in.ac.iitm.guide.backlink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.backup.ArticleArchive;
import in.ac.iitm.guide.moderate.internal.ModerationService;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-006 through the two paths that write an article's body today — the importer and the moderator's
 * approval — the event they publish, {@code backlink}'s table, and the article page that reads it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BacklinkFlowTest {

    private static final OffsetDateTime MONDAY = OffsetDateTime.parse("2026-09-28T10:00:00+05:30");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleArchive archive;

    @Autowired
    private ModerationService moderation;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    private int numbers;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM revision");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-006
    void a_published_article_linking_to_another_appears_under_what_links_here() throws Exception {
        imported("Hostel Life", "Rooms and mess.");
        imported("Arriving", "Then read [[hostel life]].");

        var page = page("/articles/hostel-life");

        assertThat(page).contains("What links here").contains("href=\"/articles/arriving\"");
    }

    @Test
    // trace:FR-006
    void a_link_from_a_pending_submission_does_not_appear() throws Exception {
        imported("Hostel Life", "Rooms and mess.");
        submission(
                SubmissionType.NEW_ARTICLE, null, "Arriving", "Then read [[Hostel Life]].", SubmissionStatus.PENDING);

        var page = page("/articles/hostel-life");

        assertThat(page).doesNotContain("What links here");
    }

    @Test
    // trace:FR-006
    void a_link_from_a_rejected_submission_does_not_appear() throws Exception {
        imported("Hostel Life", "Rooms and mess.");
        submission(
                SubmissionType.NEW_ARTICLE, null, "Arriving", "Then read [[Hostel Life]].", SubmissionStatus.REJECTED);

        var page = page("/articles/hostel-life");

        assertThat(page).doesNotContain("What links here");
    }

    @Test
    // trace:FR-006
    void a_link_from_a_removed_article_does_not_appear() throws Exception {
        imported("Hostel Life", "Rooms and mess.");
        imported("Arriving", "Then read [[Hostel Life]].");
        jdbc.update("UPDATE article SET removed_at = CURRENT_TIMESTAMP WHERE slug = 'arriving'");

        var page = page("/articles/hostel-life");

        assertThat(page).doesNotContain("href=\"/articles/arriving\"");
    }

    @Test
    // trace:FR-006
    void an_approved_new_article_with_a_link_appears_under_its_target() throws Exception {
        imported("Hostel Life", "Rooms and mess.");
        var number = submission(
                SubmissionType.NEW_ARTICLE, null, "Arriving", "Then read [[Hostel Life]].", SubmissionStatus.PENDING);

        moderation.approve(number, "Your first days.", List.of());

        assertThat(page("/articles/hostel-life")).contains("href=\"/articles/arriving\"");
    }

    @Test
    // trace:FR-006
    void an_approved_edit_that_removes_the_link_takes_its_article_off_the_list() throws Exception {
        imported("Hostel Life", "Rooms and mess.");
        var arriving = imported("Arriving", "Then read [[Hostel Life]].");
        var number =
                submission(SubmissionType.EDIT, arriving, "Arriving", "Nothing to read.", SubmissionStatus.PENDING);

        moderation.approve(number, "Your first days.", List.of());

        assertThat(page("/articles/hostel-life")).doesNotContain("href=\"/articles/arriving\"");
    }

    @Test
    // trace:FR-006
    void a_link_written_before_its_target_existed_appears_once_the_target_is_published() throws Exception {
        imported("Arriving", "Then read [[Hostel Life]].");
        imported("Hostel Life", "Rooms and mess.");

        var page = page("/articles/hostel-life");

        assertThat(page).contains("href=\"/articles/arriving\"");
    }

    @Test
    // trace:FR-006
    void a_renamed_article_keeps_no_backlinks_that_named_its_old_title() throws Exception {
        var hostel = imported("Hostel Life", "Rooms and mess.");
        imported("Arriving", "Then read [[Hostel Life]].");
        var number = submission(SubmissionType.EDIT, hostel, "Hostels in Chennai", "Rooms.", SubmissionStatus.PENDING);

        moderation.approve(number, "Rooms.", List.of());

        assertThat(page("/articles/hostels-in-chennai")).doesNotContain("What links here");
    }

    @Test
    // trace:FR-006
    void two_articles_linking_to_each_other_both_render_and_each_lists_the_other() throws Exception {
        // Phase 4 edge case "circular wiki links": nothing follows a link beyond one step, so a
        // cycle must not loop in the renderer or in the backlink list.
        imported("Hostel Life", "Before arriving, read [[arriving]].");
        imported("Arriving", "Then read [[hostel life]].");

        var hostel = backlinksOn("/articles/hostel-life");
        var arriving = backlinksOn("/articles/arriving");

        assertThat(hostel).contains("href=\"/articles/arriving\"").doesNotContain("href=\"/articles/hostel-life\"");
        assertThat(arriving).contains("href=\"/articles/hostel-life\"").doesNotContain("href=\"/articles/arriving\"");
    }

    @Test
    // trace:FR-006
    void an_article_nobody_links_to_shows_no_what_links_here() throws Exception {
        imported("Hostel Life", "Rooms and mess.");

        var page = page("/articles/hostel-life");

        assertThat(page).doesNotContain("What links here");
    }

    private UUID imported(String title, String body) {
        archive.importFiles(Map.of(
                title + ".md",
                "---\ntitle: \"" + title + "\"\nsummary: \"A summary.\"\ntags: []\ncreated: 2026-09-21\n"
                        + "updated: 2026-09-21\n---\n\n" + body + "\n"));
        return jdbc.queryForObject("SELECT id FROM article WHERE title = ?", UUID.class, title);
    }

    private String submission(SubmissionType type, UUID target, String title, String body, SubmissionStatus status) {
        numbers++;
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-TEST-0000-%04d".formatted(numbers));
        submission.setType(type);
        submission.setTargetArticleId(target);
        submission.setTitle(title);
        submission.setSummary("A summary.");
        submission.setBody(body);
        submission.setStatus(status);
        submission.setSubmittedAt(MONDAY);
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(tx -> entityManager.persist(submission));
        return submission.getSubmissionNumber();
    }

    /** The page's "What links here" list alone, so a link in the article's own text is not counted. */
    private String backlinksOn(String path) throws Exception {
        var page = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(page).as("the page has a What links here list").contains("class=\"backlink-list\"");
        var list = page.substring(page.indexOf("class=\"backlink-list\""));
        return list.substring(0, list.indexOf("</ul>"));
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
    }
}
