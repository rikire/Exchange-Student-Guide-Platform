package in.ac.iitm.guide.moderate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FEAT-006 through the real login, controllers, service, H2 and migrations. The moderator logs in
 * with the password the way a browser would, and every POST carries the CSRF token of the page it
 * was sent from.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ModerationFlowTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final OffsetDateTime MONDAY = OffsetDateTime.parse("2026-09-28T10:00:00+05:30");

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
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
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM report");
        jdbc.execute("DELETE FROM revision");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-014
    void of_two_pending_submissions_the_older_appears_first_in_the_queue() throws Exception {
        var newer = pending("Opening a bank account", MONDAY.plusHours(3));
        var older = pending("Getting a SIM card", MONDAY);

        var queue = page(loggedIn(), "/moderate/queue");

        assertThat(queue).contains(older.getSubmissionNumber(), newer.getSubmissionNumber());
        assertThat(queue.indexOf(older.getSubmissionNumber()))
                .as("the older submission is listed before the newer one")
                .isLessThan(queue.indexOf(newer.getSubmissionNumber()));
    }

    @Test
    // trace:FR-014
    void with_nothing_pending_the_queue_says_it_is_empty() throws Exception {
        var rejected = pending("Already turned down", MONDAY);
        decide(rejected, SubmissionStatus.REJECTED);

        var queue = page(loggedIn(), "/moderate/queue");

        assertThat(queue).contains("The queue is empty");
        assertThat(queue).doesNotContain(rejected.getSubmissionNumber());
    }

    @Test
    // trace:FR-015
    void a_pending_submission_opens_with_its_full_text() throws Exception {
        var submission = pending("Getting a SIM card", MONDAY);

        var review = page(loggedIn(), reviewPath(submission));

        assertThat(review)
                .contains(submission.getSubmissionNumber())
                .contains("Getting a SIM card")
                .contains("Where to buy one.")
                .contains("<strong>passport</strong>")
                .contains("action=\"" + reviewPath(submission) + "/approve\"")
                .contains("action=\"" + reviewPath(submission) + "/reject\"");
    }

    @Test
    // trace:FR-015
    void a_decided_submission_opens_showing_that_it_is_no_longer_pending() throws Exception {
        var submission = pending("Getting a SIM card", MONDAY);
        decide(submission, SubmissionStatus.APPROVED);

        var review = page(loggedIn(), reviewPath(submission));

        assertThat(review).contains("no longer pending").doesNotContain("/approve\"");
    }

    @Test
    // trace:FR-015
    void a_number_that_was_never_issued_is_not_found() throws Exception {
        mockMvc.perform(get("/moderate/submissions/SUB-0000-0000-0000").session(loggedIn()))
                .andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-017
    void approving_a_new_article_submission_publishes_it_as_a_new_article() throws Exception {
        var submission = pending("Getting a SIM card", MONDAY);

        var result = approve(loggedIn(), submission, "Airtel and Jio near the main gate.", "SIM", "telecom");

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/moderate/queue");
        var article = jdbc.queryForMap("SELECT * FROM article");
        assertThat(article)
                .containsEntry("TITLE", "Getting a SIM card")
                .containsEntry("SLUG", "getting-a-sim-card")
                .containsEntry("SUMMARY", "Airtel and Jio near the main gate.")
                .containsEntry("BODY", "Bring your **passport**.");
        assertThat(articleTags()).containsExactlyInAnyOrder("sim", "telecom");
        assertThat(statusOf(submission)).isEqualTo("APPROVED");
        assertThat(page(new MockHttpSession(), "/articles/getting-a-sim-card")).contains("Getting a SIM card");
    }

    @Test
    // trace:FR-017
    void approving_an_edit_submission_updates_the_existing_article_with_the_proposed_changes() throws Exception {
        var article = published("Hostel Life", "The original text.");
        var edit = pendingEdit(article, "Hostel Life in Chennai", "The corrected text.");

        var result = approve(loggedIn(), edit, "Rooms and mess.", "hostel");

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM article"))
                .containsEntry("ID", article.getId())
                .containsEntry("TITLE", "Hostel Life in Chennai")
                .containsEntry("SLUG", "hostel-life-in-chennai")
                .containsEntry("SUMMARY", "Rooms and mess.")
                .containsEntry("BODY", "The corrected text.");
        assertThat(articleTags()).containsExactly("hostel");
        assertThat(page(new MockHttpSession(), "/articles/hostel-life-in-chennai"))
                .contains("The corrected text.");
        // DEBT-010: the old address is not kept.
        mockMvc.perform(get("/articles/hostel-life")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-020
    void approving_an_edit_retains_the_articles_previous_text_as_a_revision() throws Exception {
        var article = published("Hostel Life", "The original text.");
        var edit = pendingEdit(article, "Hostel Life", "The corrected text.");

        approve(loggedIn(), edit, "Rooms and mess.");

        var revision = jdbc.queryForMap("SELECT * FROM revision");
        assertThat(revision)
                .containsEntry("ARTICLE_ID", article.getId())
                .containsEntry("TITLE", "Hostel Life")
                .containsEntry("SUMMARY", "A summary.")
                .containsEntry("BODY", "The original text.");
    }

    @Test
    // trace:FR-017
    void approving_an_already_decided_submission_is_refused() throws Exception {
        var session = loggedIn();
        var submission = pending("Getting a SIM card", MONDAY);
        var form = reviewForm(session, submission);
        decide(submission, SubmissionStatus.REJECTED);

        var result = send(session, form, reviewPath(submission) + "/approve", Map.of("summary", List.of("Summary.")));

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("no longer pending");
        assertThat(articleCount()).isZero();
        assertThat(statusOf(submission)).isEqualTo("REJECTED");
    }

    @Test
    // trace:FR-018
    void rejecting_a_pending_submission_marks_it_rejected_and_removes_it_from_the_queue() throws Exception {
        var session = loggedIn();
        var submission = pending("Getting a SIM card", MONDAY);

        var result = reject(session, submission);

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/moderate/queue");
        assertThat(statusOf(submission)).isEqualTo("REJECTED");
        assertThat(page(session, "/moderate/queue")).doesNotContain(submission.getSubmissionNumber());
    }

    @Test
    // trace:FR-018
    void rejecting_an_already_decided_submission_is_refused() throws Exception {
        var session = loggedIn();
        var submission = pending("Getting a SIM card", MONDAY);
        var form = reviewForm(session, submission);
        decide(submission, SubmissionStatus.APPROVED);

        var result = send(session, form, reviewPath(submission) + "/reject", Map.of());

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(statusOf(submission)).isEqualTo("APPROVED");
    }

    @Test
    // trace:FR-017
    void approval_is_refused_when_the_titles_address_became_taken_while_the_submission_waited() throws Exception {
        var submission = pending("Fees & Payments", MONDAY);
        published("Fees Payments", "Already here.");

        var result = approve(loggedIn(), submission, "Fees.");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("already exists");
        assertThat(articleCount()).isEqualTo(1);
        assertThat(statusOf(submission)).isEqualTo("PENDING");
    }

    @Test
    // trace:FR-017
    void an_approval_with_a_blank_summary_is_refused_on_the_review_with_what_was_typed() throws Exception {
        var submission = pending("Getting a SIM card", MONDAY);

        var result = approve(loggedIn(), submission, "   ", "telecom");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString())
                .contains("Give the article a summary.")
                .contains("value=\"telecom\"");
        assertThat(articleCount()).isZero();
        assertThat(statusOf(submission)).isEqualTo("PENDING");
    }

    @Test
    // trace:FR-017
    void an_approval_with_a_tag_too_long_to_store_is_refused_on_the_review() throws Exception {
        var submission = pending("Getting a SIM card", MONDAY);

        var result = approve(loggedIn(), submission, "Summary.", "x".repeat(65));

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString()).contains("longer than 64 characters");
        assertThat(articleCount()).isZero();
        assertThat(statusOf(submission)).isEqualTo("PENDING");
    }

    @Test
    // trace:FR-017
    // trace:FR-020
    void a_refused_edit_approval_leaves_no_revision_and_no_new_tag_behind() throws Exception {
        // The revision and a new tag are written before the address check refuses; both must go with
        // the rolled-back approval, or a refused edit would leave history for a change never made.
        var article = published("Hostel Life", "The original text.");
        published("Mess Food", "Another article.");
        var edit = pendingEdit(article, "Mess Food", "Renamed onto another article's address.");
        var before = articleTable();

        var result = approve(loggedIn(), edit, "Summary.", "a-brand-new-tag");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM revision", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tag", Integer.class))
                .isZero();
        assertThat(articleTable()).isEqualTo(before);
        assertThat(statusOf(edit)).isEqualTo("PENDING");
    }

    @Test
    // trace:FR-017
    // trace:FR-018
    void no_path_but_approval_changes_the_published_table() throws Exception {
        // The invariant the slice exists for (roadmap phase 3, `moderate`). Each attempt below is one
        // that must not publish; the table is compared whole, so an update counts as well as an insert.
        var article = published("Hostel Life", "The original text.");
        var edit = pendingEdit(article, "Hostel Life", "Vandalised.");
        var fresh = pending("Unreviewed", MONDAY);
        var before = articleTable();

        var anonymous = new MockHttpSession();
        var loginPage =
                mockMvc.perform(get("/moderate/login").session(anonymous)).andReturn();
        var withoutLogin = send(anonymous, loginPage, reviewPath(fresh) + "/approve", Map.of("summary", List.of("S.")));
        assertThat(withoutLogin.getResponse().getRedirectedUrl()).endsWith("/moderate/login");

        var session = loggedIn();
        var withoutToken = mockMvc.perform(
                        post(reviewPath(fresh) + "/approve").session(session).param("summary", "S."))
                .andReturn();
        assertThat(withoutToken.getResponse().getStatus()).isEqualTo(403);

        var editForm = reviewForm(session, edit);
        assertThat(reject(session, edit).getResponse().getStatus()).isEqualTo(302);
        var secondDecision = send(session, editForm, reviewPath(edit) + "/approve", Map.of("summary", List.of("S.")));
        assertThat(secondDecision.getResponse().getStatus()).isEqualTo(409);
        assertThat(reject(session, fresh).getResponse().getStatus()).isEqualTo(302);

        assertThat(articleTable()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM revision", Integer.class))
                .isZero();
    }

    // --- The moderator's side, as a browser does it.

    /** Logs in with the password through the real form, and returns the session that holds it. */
    private MockHttpSession loggedIn() throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var result = send(session, form, "/moderate/login", Map.of("password", List.of(PASSWORD)));
        assertThat(result.getResponse().getRedirectedUrl())
                .as("the login succeeded")
                .isEqualTo("/moderate/queue");
        // The session id changes on login; MockMvc hands the same object back under the new id.
        return (MockHttpSession) result.getRequest().getSession();
    }

    private MvcResult approve(MockHttpSession session, Submission submission, String summary, String... tags)
            throws Exception {
        var form = reviewForm(session, submission);
        return send(
                session,
                form,
                reviewPath(submission) + "/approve",
                Map.of("summary", List.of(summary), "tags", List.of(tags)));
    }

    private MvcResult reject(MockHttpSession session, Submission submission) throws Exception {
        return send(session, reviewForm(session, submission), reviewPath(submission) + "/reject", Map.of());
    }

    private MvcResult reviewForm(MockHttpSession session, Submission submission) throws Exception {
        return mockMvc.perform(get(reviewPath(submission)).session(session))
                .andExpect(status().isOk())
                .andReturn();
    }

    /** Posts with the CSRF token and cookies of the page the form was on. */
    private MvcResult send(MockHttpSession session, MvcResult form, String action, Map<String, List<String>> params)
            throws Exception {
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the page carries a CSRF token").isTrue();
        var request = post(action).session(session).param("_csrf", matcher.group(1));
        params.forEach((name, values) -> {
            if (!values.isEmpty()) {
                request.param(name, values.toArray(String[]::new));
            }
        });
        var cookies = form.getResponse().getCookies();
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request).andReturn();
    }

    private String page(MockHttpSession session, String path) throws Exception {
        return mockMvc.perform(get(path).session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String reviewPath(Submission submission) {
        return "/moderate/submissions/" + submission.getSubmissionNumber();
    }

    // --- The database, arranged and read directly.

    private Submission pending(String title, OffsetDateTime submittedAt) {
        var submission = aSubmission(SubmissionType.NEW_ARTICLE, null, title, "Bring your **passport**.");
        submission.setSubmittedAt(submittedAt);
        return save(submission);
    }

    private Submission pendingEdit(Article article, String title, String body) {
        return save(aSubmission(SubmissionType.EDIT, article.getId(), title, body));
    }

    private Submission aSubmission(SubmissionType type, UUID target, String title, String body) {
        var submission = new Submission();
        submission.setSubmissionNumber(nextNumber());
        submission.setType(type);
        submission.setTargetArticleId(target);
        submission.setTitle(title);
        submission.setSummary("Where to buy one.");
        submission.setBody(body);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(MONDAY);
        submission.setTags(new HashSet<>());
        return submission;
    }

    private int numbers;

    /** A well-formed number (ADR-0011); the tests need them distinct, not random. */
    private String nextNumber() {
        numbers++;
        return "SUB-TEST-0000-%04d".formatted(numbers);
    }

    private Article published(String title, String body) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody(body);
        article.setPublishedAt(MONDAY);
        article.setUpdatedAt(MONDAY);
        article.setTags(Set.<Tag>of());
        transaction.executeWithoutResult(status -> entityManager.persist(article));
        return article;
    }

    private Submission save(Submission submission) {
        transaction.executeWithoutResult(status -> entityManager.persist(submission));
        return submission;
    }

    private void decide(Submission submission, SubmissionStatus status) {
        jdbc.update(
                "UPDATE submission SET status = ?, decided_at = CURRENT_TIMESTAMP WHERE id = ?",
                status.name(),
                submission.getId());
    }

    private String statusOf(Submission submission) {
        return jdbc.queryForObject("SELECT status FROM submission WHERE id = ?", String.class, submission.getId());
    }

    private int articleCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM article", Integer.class);
    }

    private List<Map<String, Object>> articleTable() {
        return jdbc.queryForList("SELECT * FROM article ORDER BY id");
    }

    private List<String> articleTags() {
        return jdbc.queryForList("SELECT t.name FROM article_tag at JOIN tag t ON t.id = at.tag_id", String.class);
    }
}
