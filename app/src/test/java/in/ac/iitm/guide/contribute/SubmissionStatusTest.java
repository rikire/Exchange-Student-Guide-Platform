package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-012 through the controller, the service, H2 and the migrations: a contributor holding a
 * submission number sees its status, and nothing else about it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionStatusTest {

    private static final OffsetDateTime MONDAY = OffsetDateTime.parse("2026-09-28T10:00:00+05:30");
    private static final String BODY = "Bring your **passport** to the counter.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    private int numbers;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
    }

    @Test
    // trace:FR-012
    void the_status_page_without_a_number_offers_the_lookup_form() throws Exception {
        var response = lookUp(null);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).containsPattern("<input[^>]*name=\"number\"");
    }

    @Test
    // trace:FR-012
    void a_blank_number_shows_the_form_and_no_answer() throws Exception {
        var response = lookUp("   ");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).doesNotContain("No such submission was found");
    }

    @Test
    // trace:FR-012
    void a_pending_submission_shows_that_it_is_pending() throws Exception {
        var submission = saved(SubmissionType.NEW_ARTICLE, null, "Getting a SIM card", SubmissionStatus.PENDING, null);

        var response = lookUp(submission.getSubmissionNumber());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("status-pending");
    }

    @Test
    // trace:FR-012
    void an_approved_new_article_shows_that_it_is_approved_with_a_link_to_the_article() throws Exception {
        published("Getting a SIM card");
        var submission = saved(SubmissionType.NEW_ARTICLE, null, "Getting a SIM card", SubmissionStatus.APPROVED, null);

        var page = lookUp(submission.getSubmissionNumber()).getContentAsString();

        assertThat(page).contains("status-approved");
        assertThat(page).contains("href=\"/articles/getting-a-sim-card\"");
    }

    @Test
    // trace:FR-012
    void an_approved_edit_links_to_the_article_it_changed_under_its_current_title() throws Exception {
        var article = published("Mobile phones in India");
        var submission =
                saved(SubmissionType.EDIT, article.getId(), "Getting a SIM card", SubmissionStatus.APPROVED, null);

        var page = lookUp(submission.getSubmissionNumber()).getContentAsString();

        assertThat(page).contains("href=\"/articles/mobile-phones-in-india\"");
    }

    @Test
    // trace:FR-012
    void a_rejected_submission_with_a_reason_shows_the_reason() throws Exception {
        var submission = saved(
                SubmissionType.NEW_ARTICLE,
                null,
                "Getting a SIM card",
                SubmissionStatus.REJECTED,
                "Propose it as an edit to the existing article instead.");

        var page = lookUp(submission.getSubmissionNumber()).getContentAsString();

        assertThat(page).contains("status-rejected");
        assertThat(page).contains("Propose it as an edit to the existing article instead.");
    }

    @Test
    // trace:FR-012
    void a_rejected_submission_without_a_reason_shows_no_reason_block() throws Exception {
        var submission = saved(SubmissionType.NEW_ARTICLE, null, "Getting a SIM card", SubmissionStatus.REJECTED, null);

        var page = lookUp(submission.getSubmissionNumber()).getContentAsString();

        assertThat(page).contains("status-rejected");
        assertThat(page).doesNotContain("Reason given by the moderator");
    }

    @Test
    // trace:FR-012
    void a_number_never_issued_shows_that_no_such_submission_was_found() throws Exception {
        var response = lookUp("SUB-TEST-9999-9999");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("No such submission was found");
    }

    @Test
    // trace:FR-012
    void text_that_cannot_be_a_number_shows_that_no_such_submission_was_found() throws Exception {
        var response = lookUp("hello");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("No such submission was found");
    }

    @Test
    // trace:FR-012
    void a_number_typed_in_lower_case_without_hyphens_and_with_spaces_around_is_found() throws Exception {
        var submission = saved(SubmissionType.NEW_ARTICLE, null, "Getting a SIM card", SubmissionStatus.PENDING, null);
        var typed = "  " + submission.getSubmissionNumber().replace("-", "").toLowerCase() + " ";

        var response = lookUp(typed);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains(submission.getSubmissionNumber());
    }

    @Test
    // trace:FR-012
    void the_status_page_does_not_show_the_text_of_the_submission() throws Exception {
        var submission = saved(SubmissionType.NEW_ARTICLE, null, "Getting a SIM card", SubmissionStatus.PENDING, null);

        var page = lookUp(submission.getSubmissionNumber()).getContentAsString();

        assertThat(page).doesNotContain("passport");
    }

    @Test
    // trace:FR-012
    void the_confirmation_page_links_to_the_status_of_its_number() throws Exception {
        var submission = saved(SubmissionType.NEW_ARTICLE, null, "Getting a SIM card", SubmissionStatus.PENDING, null);
        var number = submission.getSubmissionNumber();

        var page = mockMvc.perform(get("/submissions/" + number + "/confirmation"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page).contains("href=\"/submissions/status?number=" + number + "\"");
    }

    private MockHttpServletResponse lookUp(String number) throws Exception {
        var request = get("/submissions/status");
        if (number != null) {
            request.param("number", number);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private Submission saved(
            SubmissionType type, UUID target, String title, SubmissionStatus status, String rejectionReason) {
        numbers++;
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-TEST-0000-%04d".formatted(numbers));
        submission.setType(type);
        submission.setTargetArticleId(target);
        submission.setTitle(title);
        submission.setSummary("Where to buy one.");
        submission.setBody(BODY);
        submission.setStatus(status);
        submission.setRejectionReason(rejectionReason);
        submission.setSubmittedAt(MONDAY);
        submission.setDecidedAt(status == SubmissionStatus.PENDING ? null : MONDAY.plusDays(1));
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(tx -> entityManager.persist(submission));
        return submission;
    }

    private Article published(String title) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody("Where to buy one.");
        article.setPublishedAt(MONDAY);
        article.setUpdatedAt(MONDAY);
        article.setTags(Set.<Tag>of());
        transaction.executeWithoutResult(tx -> entityManager.persist(article));
        return article;
    }
}
