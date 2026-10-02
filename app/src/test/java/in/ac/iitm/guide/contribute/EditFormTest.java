package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fix 3.6's edit form (F-11, F-16): it names the article it edits, "Cancel" leads back to that
 * article, and a title taken by a different article is refused with that article's title.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EditFormTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

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
    // trace:FR-011
    void the_edit_form_is_headed_with_the_title_of_the_article_it_edits() throws Exception {
        publish("Hostel Life");

        var page = page("/articles/hostel-life/edit");

        assertThat(page).containsPattern("<h1[^>]*>\\s*Propose an edit to <em>Hostel Life</em>\\s*</h1>");
        assertThat(page).contains("<title>Propose an edit to Hostel Life — Exchange Student Guide</title>");
    }

    @Test
    // trace:FR-011
    void a_refused_edit_is_still_headed_with_the_articles_own_title_not_the_one_typed() throws Exception {
        publish("Hostel Life");

        var page = refusedEdit("hostel-life", "");

        assertThat(page).containsPattern("Propose an edit to <em>Hostel Life</em>");
    }

    @Test
    // trace:FR-011
    void cancel_on_the_edit_form_leads_back_to_the_article() throws Exception {
        publish("Hostel Life");

        assertThat(page("/articles/hostel-life/edit"))
                .containsPattern("<a[^>]*href=\"/articles/hostel-life\"[^>]*>Cancel</a>");
    }

    @Test
    // trace:FR-010
    void cancel_on_the_new_article_form_leads_to_the_guide() throws Exception {
        assertThat(page("/submit")).containsPattern("<a[^>]*href=\"/\"[^>]*>Cancel</a>");
    }

    @Test
    // trace:FR-011
    void a_title_taken_by_a_different_article_is_refused_naming_that_article() throws Exception {
        publish("Hostel Life");
        publish("Mess Food");

        var page = refusedEdit("mess-food", "HOSTEL life");

        assertThat(page).contains("“Hostel Life” already has this title.");
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String refusedEdit(String address, String title) throws Exception {
        var form = mockMvc.perform(get("/articles/" + address + "/edit")).andReturn();
        var token = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        var request = multipart("/articles/" + address + "/edits");
        request.session((MockHttpSession) form.getRequest().getSession())
                .param("_csrf", token.group(1))
                .param("title", title)
                .param("summary", "A summary.")
                .param("body", "Text.");
        var cookies = form.getResponse().getCookies();
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        var result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        return result.getResponse().getContentAsString();
    }

    private void publish(String title) {
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody("Text.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }
}
