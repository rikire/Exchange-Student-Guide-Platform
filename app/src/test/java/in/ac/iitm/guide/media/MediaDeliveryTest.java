package in.ac.iitm.guide.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code GET /media/{id}} (FEAT-009): who gets the bytes, and with which headers. The moderator logs
 * in with the password the way a browser would, as in the moderation tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MediaDeliveryTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Path ROOT = MediaTestFiles.ROOT;

    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaAssets media;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabaseAndTheRoot() throws IOException {
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article");
        MediaTestFiles.empty(ROOT);
    }

    @Test
    // trace:FR-016
    void an_asset_on_a_pending_submission_answers_404_without_a_moderator_session() throws Exception {
        var item = attach(pending(), "form.jpg", MediaTestFiles.jpeg(10, 10));

        assertThat(fetch(item).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    // trace:FR-016
    void an_asset_on_a_rejected_submission_answers_404_without_a_moderator_session() throws Exception {
        var submission = pending();
        var item = attach(submission, "form.jpg", MediaTestFiles.jpeg(10, 10));
        jdbc.update("UPDATE submission SET status = 'REJECTED' WHERE id = ?", submission);

        assertThat(fetch(item).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    // trace:FR-015
    void an_asset_on_a_pending_submission_is_returned_to_a_moderator() throws Exception {
        var pdf = MediaTestFiles.pdf(2_000);
        var item = attach(pending(), "checklist.pdf", pdf);

        var result = mockMvc.perform(get(item.href()).session(loggedIn())).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    // trace:FR-001
    void an_asset_on_a_published_article_is_returned_to_anyone_with_nosniff() throws Exception {
        var item = published("form.jpg", MediaTestFiles.jpeg(10, 10));

        var response = fetch(item).getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    // trace:FR-001
    void an_asset_on_a_removed_article_answers_404() throws Exception {
        var item = published("form.jpg", MediaTestFiles.jpeg(10, 10));
        jdbc.update("UPDATE article SET removed_at = CURRENT_TIMESTAMP");

        assertThat(fetch(item).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    // trace:FR-001
    void a_document_is_returned_as_an_attachment_and_a_photo_is_not() throws Exception {
        var document = published("FRRO checklist.pdf", MediaTestFiles.pdf(2_000));
        var photo = published("form.jpg", MediaTestFiles.jpeg(10, 10));

        assertThat(fetch(document).getResponse().getHeader("Content-Disposition"))
                .startsWith("attachment")
                .contains("FRRO");
        assertThat(fetch(photo).getResponse().getHeader("Content-Disposition")).isNull();
    }

    @Test
    // trace:FR-001
    void a_range_request_for_a_video_answers_206_with_that_range() throws Exception {
        var item = published("walk.mp4", MediaTestFiles.mp4(2_000));

        var response = mockMvc.perform(get(item.href()).header("Range", "bytes=0-99"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getHeader("Content-Range")).isEqualTo("bytes 0-99/2000");
        assertThat(response.getContentAsByteArray()).hasSize(100);
    }

    @Test
    // trace:FR-001
    void an_unknown_id_and_one_that_is_not_an_id_answer_404() throws Exception {
        assertThat(mockMvc.perform(get("/media/" + UUID.randomUUID()))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(404);
        assertThat(mockMvc.perform(get("/media/not-an-id"))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(404);
    }

    private MvcResult fetch(MediaItem item) throws Exception {
        return mockMvc.perform(get(item.href())).andReturn();
    }

    private MediaItem attach(UUID submission, String name, byte[] bytes) {
        return media.attach(submission, new Upload(name, bytes.length, new ByteArrayResource(bytes)));
    }

    private MediaItem published(String name, byte[] bytes) {
        var submission = pending();
        var item = attach(submission, name, bytes);
        media.moveToArticle(submission, article());
        return item;
    }

    private MockHttpSession loggedIn() throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the login page carries a CSRF token").isTrue();
        var request = post("/moderate/login")
                .session(session)
                .param("_csrf", matcher.group(1))
                .param("password", PASSWORD);
        if (form.getResponse().getCookies().length > 0) {
            request.cookie(form.getResponse().getCookies());
        }
        var result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getRedirectedUrl())
                .as("the login succeeded")
                .isEqualTo("/moderate/queue");
        return (MockHttpSession) result.getRequest().getSession();
    }

    private int numbers;

    private UUID pending() {
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-TEST-0000-%04d".formatted(++numbers));
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Getting a SIM card " + numbers);
        submission.setSummary("Where to buy one.");
        submission.setBody("Take your passport.");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(status -> entityManager.persist(submission));
        return submission.getId();
    }

    private UUID article() {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle("Registering with FRRO " + numbers);
        article.setSlug("registering-with-frro-" + numbers);
        article.setSummary("A summary.");
        article.setBody("Text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(new HashSet<>());
        transaction.executeWithoutResult(status -> entityManager.persist(article));
        return article.getId();
    }
}
