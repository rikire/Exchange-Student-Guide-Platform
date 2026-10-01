package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The server's half of FR-027: the preview the editor asks for as the contributor types. Posts carry
 * the CSRF token of the form page, as the editor's script sends it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EditorPreviewTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    /** Hindi, Tamil, a combining sequence and an emoji with a modifier: each has broken editors before. */
    private static final String EVERY_SCRIPT = "छात्रावास में पंजीकरण। விடுதி பதிவு. é 👋🏽";

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
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
    }

    @Test
    // trace:FR-010
    void the_form_warns_that_a_video_may_say_where_it_was_filmed() throws Exception {
        // Walkthrough fix 1.4 (the human, 1 Oct): photos lose their metadata when re-encoded, videos do not.
        var form = mockMvc.perform(get("/submit")).andReturn().getResponse().getContentAsString();

        assertThat(form).contains("A video keeps what the camera recorded, which can include where it was filmed.");
    }

    @Test
    // trace:FR-027
    void the_form_shows_the_wiki_link_syntax_with_its_brackets() throws Exception {
        // Walkthrough F-5: Thymeleaf read [[Title]] in the hint as its own inline expression.
        var form = mockMvc.perform(get("/submit")).andReturn().getResponse().getContentAsString();

        assertThat(form).contains("Link to another article with [[Title]].");
    }

    @Test
    // trace:FR-027
    void the_preview_shows_the_servers_rendering_with_wiki_links_resolved() throws Exception {
        publish("Hostel Life");

        var html = preview("**Bring** your [[hostel life]] papers and the [[No Such Page]] form.");

        assertThat(html)
                .contains("<strong>Bring</strong>")
                .contains("<a href=\"/articles/hostel-life\" class=\"wikilink\">hostel life</a>")
                .contains("<span class=\"wikilink wikilink-missing\">No Such Page</span>");
    }

    @Test
    // trace:FR-027
    void the_preview_is_the_article_page_rendering_so_raw_html_is_escaped() throws Exception {
        var html = preview("<script>alert(1)</script>");

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    // trace:FR-027
    void text_in_any_script_is_shown_by_the_preview_and_stored_unchanged_by_the_submission() throws Exception {
        var body = "## " + EVERY_SCRIPT + "\n\n" + EVERY_SCRIPT;

        var html = preview(body);
        submit(body);

        // The heading carries its id since fix 3.5; the text is what this test is about.
        assertThat(html).containsPattern("<h2[^>]*>" + EVERY_SCRIPT + "</h2>").contains("<p>" + EVERY_SCRIPT + "</p>");
        assertThat(jdbc.queryForObject("SELECT body FROM submission", String.class))
                .isEqualTo(body);
    }

    @Test
    // trace:FR-027
    void a_body_at_the_preview_limit_is_rendered_and_one_character_over_it_is_refused() throws Exception {
        var atTheLimit = "a".repeat(100_000);

        assertThat(previewResult(atTheLimit).getResponse().getStatus()).isEqualTo(200);
        assertThat(previewResult(atTheLimit + "a").getResponse().getStatus()).isEqualTo(413);
    }

    @Test
    // trace:FR-027
    void a_preview_without_a_csrf_token_is_refused() throws Exception {
        mockMvc.perform(post("/contribute/preview").param("body", "Text.")).andExpect(status().isForbidden());
    }

    private String preview(String body) throws Exception {
        var result = previewResult(body);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return result.getResponse().getContentAsString();
    }

    /** Takes the token from the form page, as the editor's script does, and posts the body with it. */
    private MvcResult previewResult(String body) throws Exception {
        var form = mockMvc.perform(get("/submit")).andExpect(status().isOk()).andReturn();
        var token = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(token.find()).as("the form carries a CSRF token").isTrue();

        var request = post("/contribute/preview")
                .session((MockHttpSession) form.getRequest().getSession())
                .param("_csrf", token.group(1))
                .param("body", body);
        var cookies = form.getResponse().getCookies();
        return mockMvc.perform(cookies.length == 0 ? request : request.cookie(cookies))
                .andReturn();
    }

    private void submit(String body) throws Exception {
        var form = mockMvc.perform(get("/submit")).andExpect(status().isOk()).andReturn();
        var token = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(token.find()).as("the form carries a CSRF token").isTrue();

        var request = post("/submissions")
                .session((MockHttpSession) form.getRequest().getSession())
                .param("_csrf", token.group(1))
                .param("title", "In every script")
                .param("summary", "A summary.")
                .param("body", body);
        var cookies = form.getResponse().getCookies();
        mockMvc.perform(cookies.length == 0 ? request : request.cookie(cookies)).andExpect(status().isFound());
    }

    private void publish(String title) {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody("The text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(Set.of());
        transaction.executeWithoutResult(status -> entityManager.persist(article));
    }
}
