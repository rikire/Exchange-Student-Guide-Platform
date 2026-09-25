package in.ac.iitm.guide.articleview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Goes through the real controller, the real repository, H2 and the real migrations. The test itself
 * is not transactional: rows are committed and the page reads them in its own transaction, so a lazy
 * association the controller forgot to fetch fails here as it would in production, instead of being
 * quietly loaded inside the test's own session.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleControllerTest {

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
    // trace:FR-001
    void a_published_article_is_shown_with_its_title_body_and_tags() throws Exception {
        publish("Registering with FRRO", "Register within **14 days** of arrival.", "visa", "admin");

        var page = html(get("/articles/registering-with-frro"));

        assertThat(page)
                .contains("Registering with FRRO")
                .contains("Register within <strong>14 days</strong> of arrival.")
                .contains(">visa<")
                .contains(">admin<");
    }

    @Test
    // trace:FR-001
    void an_address_that_matches_no_article_does_not_resolve() throws Exception {
        publish("Hostel Life", "Text.");

        mockMvc.perform(get("/articles/no-such-article")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-001
    void a_removed_article_does_not_resolve() throws Exception {
        var article = anArticle("Old Rules", "Text.");
        article.setRemovedAt(OffsetDateTime.now());
        save(article);

        mockMvc.perform(get("/articles/old-rules")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-001
    void a_submission_not_yet_approved_does_not_resolve() throws Exception {
        saveSubmission("Pending Guide", SubmissionStatus.PENDING);

        mockMvc.perform(get("/articles/pending-guide")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-001
    void a_rejected_submission_does_not_resolve() throws Exception {
        saveSubmission("Rejected Guide", SubmissionStatus.REJECTED);

        mockMvc.perform(get("/articles/rejected-guide")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-001
    void the_address_is_matched_whatever_the_letter_case() throws Exception {
        publish("Hostel Life", "Text.");

        mockMvc.perform(get("/articles/HOSTEL-Life")).andExpect(status().isOk());
    }

    @Test
    // trace:FR-001
    void a_title_in_devanagari_is_served_at_its_percent_encoded_address() throws Exception {
        publish("छात्रावास जीवन", "पाठ।");

        var path = ArticleAddress.pathOf(ArticleAddress.slugOf("छात्रावास जीवन").orElseThrow());

        // A URI, not a template string: get(String) would encode the "%" of an already-encoded path
        // a second time, which no browser does.
        assertThat(html(get(URI.create(path)))).contains("छात्रावास जीवन").contains("पाठ।");
    }

    @Test
    // trace:FR-002
    void a_wiki_link_to_a_published_article_matches_its_title_in_any_letter_case() throws Exception {
        publish("Hostel Life", "Text.");
        publish("Arrival", "Read [[hostel LIFE]] next.");

        var page = html(get("/articles/arrival"));

        assertThat(page).contains("<a href=\"/articles/hostel-life\" class=\"wikilink\">hostel LIFE</a>");
    }

    @Test
    // trace:FR-002
    void a_wiki_link_matches_a_title_whatever_the_spacing_and_punctuation_between_its_words() throws Exception {
        publish("Fees & Payments", "Text.");
        publish("Arrival", "Pay via [[fees   payments]] or [[Fees-Payments]].");

        var page = html(get("/articles/arrival"));

        assertThat(page)
                .contains("<a href=\"/articles/fees-payments\" class=\"wikilink\">fees   payments</a>")
                .contains("<a href=\"/articles/fees-payments\" class=\"wikilink\">Fees-Payments</a>");
    }

    @Test
    // trace:FR-002
    void a_wiki_link_with_words_shows_the_words_and_leads_to_the_titled_article() throws Exception {
        publish("Applying for Your Student Visa", "Text.");
        publish("Registering with FRRO", "After [[Applying for Your Student Visa|your student visa]].");

        var page = html(get("/articles/registering-with-frro"));

        assertThat(page)
                .contains(
                        "<a href=\"/articles/applying-for-your-student-visa\" class=\"wikilink\">your student visa</a>");
    }

    @Test
    // trace:FR-004
    void a_wiki_link_to_no_article_is_shown_red() throws Exception {
        publish("Arrival", "Read [[Nowhere Yet]] next.");

        var page = html(get("/articles/arrival"));

        assertThat(page).contains("<span class=\"wikilink wikilink-missing\">Nowhere Yet</span>");
    }

    @Test
    // trace:FR-004
    void a_wiki_link_to_a_removed_article_is_shown_red() throws Exception {
        var removed = anArticle("Old Rules", "Text.");
        removed.setRemovedAt(OffsetDateTime.now());
        save(removed);
        publish("Arrival", "Read [[Old Rules]] next.");

        var page = html(get("/articles/arrival"));

        assertThat(page).contains("wikilink-missing");
    }

    @Test
    // trace:FR-001
    void raw_html_in_a_body_reaches_the_page_as_text() throws Exception {
        publish("Arrival", "Hello <script>alert(1)</script> world");

        var page = html(get("/articles/arrival"));

        assertThat(page).doesNotContain("<script>alert(1)").contains("&lt;script&gt;");
    }

    @Test
    // trace:FR-001
    void html_characters_in_a_title_are_escaped_in_the_heading() throws Exception {
        publish("Fees <b>& Payments", "Text.");

        var page = html(
                get("/articles/" + ArticleAddress.slugOf("Fees <b>& Payments").orElseThrow()));

        assertThat(page).contains("Fees &lt;b&gt;&amp; Payments").doesNotContain("<b>&");
    }

    // ---- fixtures: named after what they make, so a test states only what matters to it ----

    private String html(RequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void publish(String title, String body, String... tags) {
        var article = anArticle(title, body);
        var tagRows = new HashSet<Tag>();
        for (var name : tags) {
            var tag = new Tag();
            tag.setName(name);
            tagRows.add(tag);
        }
        transaction.executeWithoutResult(status -> {
            tagRows.forEach(entityManager::persist);
            article.setTags(tagRows);
            entityManager.persist(article);
        });
    }

    private Article anArticle(String title, String body) {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody(body);
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(Set.of());
        return article;
    }

    private void save(Article article) {
        transaction.executeWithoutResult(status -> entityManager.persist(article));
    }

    private void saveSubmission(String title, SubmissionStatus status) {
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-" + Math.abs(title.hashCode()));
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle(title);
        submission.setSummary("A summary.");
        submission.setBody("Text.");
        submission.setStatus(status);
        submission.setSubmittedAt(OffsetDateTime.now());
        transaction.executeWithoutResult(s -> entityManager.persist(submission));
    }
}
