package in.ac.iitm.guide.moderate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import in.ac.iitm.guide.media.MediaTestFiles;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.OffsetDateTime;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-023 and FR-024 through the real login and controllers: the moderator writes or edits an article
 * and it is live at once, never pending in the queue. The media limits are the tests' own (16 KB a
 * PDF, config/application.yml).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DirectPublishingTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry registry) {
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

    @AfterEach
    void clearTheMediaRoot() throws IOException {
        MediaTestFiles.empty(MediaTestFiles.ROOT);
    }

    @Test
    // trace:FR-023
    void a_new_article_written_by_the_moderator_is_live_at_once_and_never_pending() throws Exception {
        var session = loggedIn();
        var photo = file("sim-booth.jpg", MediaTestFiles.jpeg(30, 20));

        var result = send(session, "/moderate/write", "/moderate/articles", "Getting a SIM card", photo);

        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/articles/getting-a-sim-card");
        assertThat(mockMvc.perform(get("/articles/getting-a-sim-card"))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(200);
        assertThat(count("SELECT COUNT(*) FROM submission WHERE status = 'PENDING'"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM media_asset WHERE article_id IS NOT NULL"))
                .isEqualTo(1);
    }

    @Test
    // trace:FR-023
    void a_direct_publication_stays_in_the_history_as_approved_at_the_moment_it_was_sent() throws Exception {
        var session = loggedIn();

        send(session, "/moderate/write", "/moderate/articles", "Getting a SIM card", null);

        var row = jdbc.queryForMap("SELECT status, submitted_at, decided_at FROM submission");
        assertThat(row.get("STATUS")).isEqualTo("APPROVED");
        assertThat(row.get("DECIDED_AT")).isEqualTo(row.get("SUBMITTED_AT"));
    }

    @Test
    // trace:FR-023
    void a_title_matching_an_existing_articles_case_insensitively_is_refused() throws Exception {
        publish("Hostel Life");
        var session = loggedIn();

        var result = send(session, "/moderate/write", "/moderate/articles", "HOSTEL life", null);

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("already exists");
        assertThat(count("SELECT COUNT(*) FROM article")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM submission")).isZero();
    }

    @Test
    // trace:FR-023
    void a_new_article_with_an_attachment_over_its_limit_is_refused_with_a_message() throws Exception {
        var session = loggedIn();
        var large = file("big.pdf", MediaTestFiles.pdf(16 * 1024 + 1));

        var result = send(session, "/moderate/write", "/moderate/articles", "Getting a SIM card", large);

        assertRefused(result, "larger than 16 KB");
        assertThat(count("SELECT COUNT(*) FROM article")).isZero();
    }

    @Test
    // trace:FR-023
    void a_new_article_with_an_attachment_not_of_an_accepted_type_is_refused_with_a_message() throws Exception {
        var session = loggedIn();
        var page = file("form.jpg", "<!DOCTYPE html><html><body>hi</body></html>".getBytes());

        var result = send(session, "/moderate/write", "/moderate/articles", "Getting a SIM card", page);

        assertRefused(result, "not an accepted type");
        assertThat(count("SELECT COUNT(*) FROM article")).isZero();
    }

    @Test
    // trace:FR-024
    void an_edit_by_the_moderator_is_live_at_once_and_the_text_it_replaces_is_kept_as_a_revision() throws Exception {
        publish("Registering with FRRO");
        var session = loggedIn();

        var result = send(
                session,
                "/moderate/articles/registering-with-frro/edit",
                "/moderate/articles/registering-with-frro/edits",
                "Registering with the FRRO",
                null);

        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/articles/registering-with-the-frro");
        assertThat(jdbc.queryForObject("SELECT body FROM article", String.class))
                .isEqualTo("The text that goes with it.");
        assertThat(jdbc.queryForMap("SELECT title, body FROM revision"))
                .containsEntry("TITLE", "Registering with FRRO")
                .containsEntry("BODY", "The old text.");
        assertThat(count("SELECT COUNT(*) FROM submission WHERE status = 'PENDING'"))
                .isZero();
    }

    @Test
    // trace:FR-024
    void an_edit_with_an_attachment_over_its_limit_is_refused_with_a_message() throws Exception {
        publish("Registering with FRRO");
        var session = loggedIn();
        var large = file("big.pdf", MediaTestFiles.pdf(16 * 1024 + 1));

        var result = send(
                session,
                "/moderate/articles/registering-with-frro/edit",
                "/moderate/articles/registering-with-frro/edits",
                "Registering with FRRO",
                large);

        assertRefused(result, "larger than 16 KB");
        assertUnchanged();
    }

    @Test
    // trace:FR-024
    void an_edit_with_an_attachment_not_of_an_accepted_type_is_refused_with_a_message() throws Exception {
        publish("Registering with FRRO");
        var session = loggedIn();
        var page = file("form.jpg", "<!DOCTYPE html><html><body>hi</body></html>".getBytes());

        var result = send(
                session,
                "/moderate/articles/registering-with-frro/edit",
                "/moderate/articles/registering-with-frro/edits",
                "Registering with FRRO",
                page);

        assertRefused(result, "not an accepted type");
        assertUnchanged();
    }

    @Test
    // trace:FR-024
    void a_new_title_matching_a_different_articles_case_insensitively_is_refused() throws Exception {
        publish("Registering with FRRO");
        publish("Hostel Life");
        var session = loggedIn();

        var result = send(
                session,
                "/moderate/articles/registering-with-frro/edit",
                "/moderate/articles/registering-with-frro/edits",
                "hostel LIFE",
                null);

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("already has this title");
        assertThat(jdbc.queryForObject("SELECT body FROM article WHERE slug = 'registering-with-frro'", String.class))
                .isEqualTo("The old text.");
        assertThat(count("SELECT COUNT(*) FROM revision")).isZero();
    }

    @Test
    // trace:FR-024
    void the_article_page_offers_the_moderator_to_edit_it_now_and_a_reader_to_propose_an_edit() throws Exception {
        publish("Registering with FRRO");

        var asModerator = mockMvc.perform(get("/articles/registering-with-frro").session(loggedIn()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var asReader = mockMvc.perform(get("/articles/registering-with-frro"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(asModerator)
                .contains("href=\"/moderate/articles/registering-with-frro/edit\"")
                .contains("Edit now")
                .doesNotContain("Propose an edit");
        assertThat(asReader)
                .contains("Propose an edit")
                .doesNotContain("/moderate/articles/registering-with-frro/edit");
    }

    @Test
    // trace:FR-023
    void the_moderators_header_links_to_writing_an_article() throws Exception {
        var queue = mockMvc.perform(get("/moderate/queue").session(loggedIn()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(queue).contains("href=\"/moderate/write\"");
    }

    @Test
    // trace:FR-024
    void the_report_inbox_leads_to_editing_the_reported_article() throws Exception {
        publish("Registering with FRRO");
        jdbc.update(
                "INSERT INTO report (id, article_id, message, reported_at) "
                        + "SELECT ?, id, 'Out of date.', CURRENT_TIMESTAMP FROM article",
                UUID.randomUUID());

        var inbox = mockMvc.perform(get("/moderate/reports").session(loggedIn()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(inbox)
                .contains("href=\"/moderate/articles/registering-with-frro/edit\"")
                .contains("Edit article");
    }

    private void assertRefused(MvcResult result, String message) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString())
                .contains(message)
                .as("the form again, with what was typed")
                .contains("The text that goes with it.");
        assertThat(count("SELECT COUNT(*) FROM submission")).isZero();
        assertThat(count("SELECT COUNT(*) FROM media_asset")).isZero();
    }

    private void assertUnchanged() {
        assertThat(jdbc.queryForObject("SELECT body FROM article", String.class))
                .isEqualTo("The old text.");
        assertThat(count("SELECT COUNT(*) FROM revision")).isZero();
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("attachment", name, "application/octet-stream", bytes);
    }

    /** Opens the form, then sends it with this title, a summary, a body, one tag and the file if any. */
    private MvcResult send(
            MockHttpSession session, String formPath, String action, String title, MockMultipartFile attachment)
            throws Exception {
        var form = mockMvc.perform(get(formPath).session(session)).andReturn();
        assertThat(form.getResponse().getStatus())
                .as("the form at %s", formPath)
                .isEqualTo(200);
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the form carries a CSRF token").isTrue();
        var request = multipart(action);
        if (attachment != null) {
            request.file(attachment);
        }
        request.session(session)
                .param("_csrf", matcher.group(1))
                .param("title", title)
                .param("summary", "A summary.")
                .param("body", "The text that goes with it.")
                .param("tags", "phone");
        if (form.getResponse().getCookies().length > 0) {
            request.cookie(form.getResponse().getCookies());
        }
        return mockMvc.perform(request).andReturn();
    }

    private MockHttpSession loggedIn() throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        var login = post("/moderate/login")
                .session(session)
                .param("_csrf", matcher.group(1))
                .param("password", PASSWORD);
        if (form.getResponse().getCookies().length > 0) {
            login.cookie(form.getResponse().getCookies());
        }
        var result = mockMvc.perform(login).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/moderate/queue");
        return (MockHttpSession) result.getRequest().getSession();
    }

    private void publish(String title) {
        transaction.executeWithoutResult(status -> {
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody("The old text.");
            article.setPublishedAt(OffsetDateTime.now());
            article.setUpdatedAt(OffsetDateTime.now());
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }
}
