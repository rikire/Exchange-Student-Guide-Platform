package in.ac.iitm.guide.moderate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-026 through the real login, controllers, H2, the search index and the migrations: the moderator
 * removes a published article from its page, after a confirmation, and it leaves every public page.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ArticleRemovalTest {

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
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM revision");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-026
    void a_removal_is_logged_with_the_articles_address(CapturedOutput output) throws Exception {
        // security.md: every destructive admin action is logged with what it affected.
        published("Hostel Life", "Rooms and mess.");

        remove(loggedIn(), "hostel-life");

        assertThat(output).contains("Removed article at address hostel-life");
    }

    @Test
    // trace:FR-026
    void a_removed_articles_route_no_longer_resolves() throws Exception {
        published("Hostel Life", "Rooms and mess.");

        remove(loggedIn(), "hostel-life");

        assertThat(response(get("/articles/hostel-life")).getStatus()).isEqualTo(404);
    }

    @Test
    // trace:FR-026
    void a_removed_article_no_longer_appears_in_search_results() throws Exception {
        published("Hostel Life", "Rooms and mess.");

        remove(loggedIn(), "hostel-life");

        assertThat(response(get("/search").param("q", "hostel")).getContentAsString())
                .doesNotContain("Hostel Life");
    }

    @Test
    // trace:FR-026
    void a_removed_article_no_longer_appears_under_its_tags() throws Exception {
        published("Hostel Life", "Rooms and mess.", "hostel");
        published("Mess Menu", "What the mess serves.", "hostel");

        remove(loggedIn(), "hostel-life");

        assertThat(response(get("/tags/hostel")).getContentAsString())
                .contains("Mess Menu")
                .doesNotContain("Hostel Life");
    }

    @Test
    // trace:FR-026
    void a_wiki_link_to_a_removed_article_renders_as_a_red_link() throws Exception {
        published("Hostel Life", "Rooms and mess.");
        published("Arriving", "Then read [[Hostel Life]].");

        remove(loggedIn(), "hostel-life");

        assertThat(response(get("/articles/arriving")).getContentAsString())
                .contains("wikilink-missing")
                .doesNotContain("href=\"/articles/hostel-life\"");
    }

    @Test
    // trace:FR-026
    void a_removed_pinned_article_leaves_the_pinned_section() throws Exception {
        published("Hostel Life", "Rooms and mess.");
        jdbc.update("UPDATE article SET pinned_at = CURRENT_TIMESTAMP");

        remove(loggedIn(), "hostel-life");

        assertThat(response(get("/")).getContentAsString()).doesNotContain("Hostel Life");
    }

    @Test
    // trace:FR-026
    void an_article_already_removed_cannot_be_removed_again() throws Exception {
        published("Hostel Life", "Rooms and mess.");
        var session = loggedIn();
        remove(session, "hostel-life");

        var confirmation = response(get(removePath("hostel-life")).session(session));

        assertThat(confirmation.getStatus()).isEqualTo(404);
    }

    @Test
    // trace:FR-026
    void the_confirmation_names_the_article_and_cancel_goes_back_to_it() throws Exception {
        published("Hostel Life", "Rooms and mess.");

        var page = response(get(removePath("hostel-life")).session(loggedIn())).getContentAsString();

        assertThat(page).contains("Remove “Hostel Life”?");
        assertThat(page).containsPattern("<a[^>]*href=\"/articles/hostel-life\"[^>]*>Cancel</a>");
    }

    @Test
    // trace:FR-026
    void the_article_page_offers_removal_to_a_moderator_only() throws Exception {
        published("Hostel Life", "Rooms and mess.");

        var toTheModerator =
                response(get("/articles/hostel-life").session(loggedIn())).getContentAsString();
        var toAReader = response(get("/articles/hostel-life")).getContentAsString();

        assertThat(toTheModerator).contains("href=\"" + removePath("hostel-life") + "\"");
        assertThat(toAReader).doesNotContain(removePath("hostel-life"));
    }

    @Test
    // trace:FR-026
    void removal_without_a_moderator_session_is_refused_and_the_article_stays() throws Exception {
        published("Hostel Life", "Rooms and mess.");
        var readersForm = mockMvc.perform(get("/submit")).andReturn();

        var result = send(new MockHttpSession(), readersForm, removePath("hostel-life"));

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).endsWith("/moderate/login");
        assertThat(response(get("/articles/hostel-life")).getStatus()).isEqualTo(200);
    }

    @Test
    // trace:FR-026
    void removal_takes_the_moderator_back_to_the_queue() throws Exception {
        published("Hostel Life", "Rooms and mess.");

        var result = remove(loggedIn(), "hostel-life");

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/moderate/queue");
    }

    private MvcResult remove(MockHttpSession session, String address) throws Exception {
        var confirmation =
                mockMvc.perform(get(removePath(address)).session(session)).andReturn();
        assertThat(confirmation.getResponse().getStatus()).isEqualTo(200);
        return send(session, confirmation, removePath(address));
    }

    private static String removePath(String address) {
        return "/moderate/articles/" + address + "/remove";
    }

    /** Posts with the CSRF token and cookies of the page the form was on. */
    private MvcResult send(MockHttpSession session, MvcResult form, String action) throws Exception {
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the page carries a CSRF token").isTrue();
        var request = post(action).session(session).param("_csrf", matcher.group(1));
        var cookies = form.getResponse().getCookies();
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request).andReturn();
    }

    private MockHttpServletResponse response(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse();
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
        var cookies = form.getResponse().getCookies();
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        var result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getRedirectedUrl())
                .as("the login succeeded")
                .isEqualTo("/moderate/queue");
        // The session id changes on login; MockMvc hands the same object back under the new id.
        return (MockHttpSession) result.getRequest().getSession();
    }

    private void published(String title, String body, String... tagNames) {
        transaction.executeWithoutResult(status -> {
            var tags = new HashSet<Tag>();
            for (var name : tagNames) {
                var existing = entityManager
                        .createQuery("select t from Tag t where t.name = :name", Tag.class)
                        .setParameter("name", name)
                        .getResultList();
                if (existing.isEmpty()) {
                    var tag = new Tag();
                    tag.setName(name);
                    entityManager.persist(tag);
                    tags.add(tag);
                } else {
                    tags.add(existing.getFirst());
                }
            }
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody(body);
            article.setPublishedAt(MONDAY);
            article.setUpdatedAt(MONDAY);
            article.setTags(tags.isEmpty() ? Set.of() : tags);
            entityManager.persist(article);
        });
    }
}
