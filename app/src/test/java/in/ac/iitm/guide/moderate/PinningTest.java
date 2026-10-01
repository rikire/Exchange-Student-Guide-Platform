package in.ac.iitm.guide.moderate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
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
 * FR-025 through the real login and controllers: the moderator pins, unpins and orders the landing
 * page's pinned articles from {@code /moderate/articles}, the HomeAdmin design screen.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PinningTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-01T12:00:00Z");

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
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
    }

    @Test
    // trace:FR-025
    void a_pinned_article_appears_in_the_landing_pages_pinned_section() throws Exception {
        publish("Registering with FRRO");
        var session = loggedIn();

        act(session, "registering-with-frro", "pin");

        assertThat(pinnedOnLanding()).contains("Registering with FRRO");
    }

    @Test
    // trace:FR-025
    void an_unpinned_article_leaves_the_pinned_section() throws Exception {
        publish("Registering with FRRO");
        var session = loggedIn();
        act(session, "registering-with-frro", "pin");

        act(session, "registering-with-frro", "unpin");

        assertThat(pinnedOnLanding()).doesNotContain("Registering with FRRO");
    }

    @Test
    // trace:FR-025
    void moving_the_third_up_puts_it_second() throws Exception {
        publish("Alpha");
        publish("Bravo");
        publish("Charlie");
        var session = loggedIn();
        act(session, "alpha", "pin");
        act(session, "bravo", "pin");
        act(session, "charlie", "pin");

        act(session, "charlie", "up");

        assertThat(order(pinnedOnLanding(), "Alpha", "Charlie", "Bravo")).isTrue();
    }

    @Test
    // trace:FR-025
    void a_newly_pinned_article_goes_after_the_ones_already_pinned() throws Exception {
        publish("Alpha");
        publish("Bravo");
        publish("Charlie");
        var session = loggedIn();
        act(session, "charlie", "pin");
        act(session, "alpha", "pin");

        act(session, "bravo", "pin");

        assertThat(order(pinnedOnLanding(), "Charlie", "Alpha", "Bravo")).isTrue();
    }

    @Test
    // trace:FR-025
    void moving_the_first_up_or_the_last_down_changes_nothing() throws Exception {
        publish("Alpha");
        publish("Bravo");
        var session = loggedIn();
        act(session, "alpha", "pin");
        act(session, "bravo", "pin");

        act(session, "alpha", "up");
        act(session, "bravo", "down");

        assertThat(order(pinnedOnLanding(), "Alpha", "Bravo")).isTrue();
    }

    @Test
    // trace:FR-025
    void the_screen_lists_the_published_articles_with_pin_and_remove() throws Exception {
        publish("Registering with FRRO");
        publish("Eating on campus");
        var session = loggedIn();
        act(session, "registering-with-frro", "pin");

        var screen = mockMvc.perform(get("/moderate/articles").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(screen).contains("Registering with FRRO", "Eating on campus");
        assertThat(screen).contains("action=\"/moderate/articles/registering-with-frro/unpin\"");
        assertThat(screen).contains("action=\"/moderate/articles/eating-on-campus/pin\"");
        assertThat(screen).contains("href=\"/moderate/articles/eating-on-campus/remove\"");
    }

    @Test
    // trace:FR-025
    void without_the_login_the_screen_redirects_and_a_pin_without_the_token_is_forbidden() throws Exception {
        publish("Registering with FRRO");

        var screen = mockMvc.perform(get("/moderate/articles")).andReturn();
        mockMvc.perform(post("/moderate/articles/registering-with-frro/pin")).andExpect(status().isForbidden());

        assertThat(screen.getResponse().getStatus()).isEqualTo(302);
        assertThat(pinnedOnLanding()).doesNotContain("Registering with FRRO");
    }

    @Test
    // trace:FR-025
    void pinning_an_address_with_no_published_article_answers_404() throws Exception {
        var session = loggedIn();

        assertThat(act(session, "no-such-article", "pin").getResponse().getStatus())
                .isEqualTo(404);
    }

    /** The landing page's pinned section, which comes before "Recently added". */
    private String pinnedOnLanding() throws Exception {
        var landing = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        var start = landing.indexOf(">Pinned<");
        return start < 0 ? "" : landing.substring(start, landing.indexOf("Recently added"));
    }

    private static boolean order(String text, String... titles) {
        var at = -1;
        for (var title : titles) {
            var next = text.indexOf(">" + title + "<");
            if (next <= at) {
                return false;
            }
            at = next;
        }
        return true;
    }

    private MvcResult act(MockHttpSession session, String address, String action) throws Exception {
        var screen = mockMvc.perform(get("/moderate/articles").session(session)).andReturn();
        var matcher = CSRF.matcher(screen.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the screen carries a CSRF token").isTrue();
        var request = post("/moderate/articles/" + address + "/" + action)
                .session(session)
                .param("_csrf", matcher.group(1));
        // The token lives in a cookie (CookieCsrfTokenRepository), sent back as a browser would.
        var cookies = screen.getResponse().getCookies();
        return mockMvc.perform(cookies.length == 0 ? request : request.cookie(cookies))
                .andReturn();
    }

    private void publish(String title) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("About " + title + ".");
        article.setBody("Text.");
        article.setPublishedAt(NOW);
        article.setUpdatedAt(NOW);
        article.setTags(new HashSet<>());
        transaction.executeWithoutResult(status -> entityManager.persist(article));
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
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/moderate/queue");
        return (MockHttpSession) result.getRequest().getSession();
    }
}
