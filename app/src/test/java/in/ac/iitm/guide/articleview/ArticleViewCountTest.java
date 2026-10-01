package in.ac.iitm.guide.articleview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
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
import org.springframework.transaction.support.TransactionTemplate;

/** FR-034's three criteria: a reader's view counts, the moderator's does not, a 404 counts nothing. */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleViewCountTest {

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
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
    }

    @Test
    // trace:FR-034
    void a_readers_view_adds_one_to_the_articles_count() throws Exception {
        publish("Registering with FRRO");

        mockMvc.perform(get("/articles/registering-with-frro")).andExpect(status().isOk());

        assertThat(views("registering-with-frro")).isOne();
    }

    @Test
    // trace:FR-034
    void the_moderators_view_is_not_counted() throws Exception {
        publish("Registering with FRRO");
        var session = loggedIn();

        mockMvc.perform(get("/articles/registering-with-frro").session(session)).andExpect(status().isOk());

        assertThat(views("registering-with-frro")).isZero();
    }

    @Test
    // trace:FR-034
    void an_address_that_answers_404_counts_nothing() throws Exception {
        publish("Registering with FRRO");

        mockMvc.perform(get("/articles/no-such-article")).andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT SUM(view_count) FROM article", Long.class))
                .isZero();
    }

    private long views(String slug) {
        return jdbc.queryForObject("SELECT view_count FROM article WHERE slug = ?", Long.class, slug);
    }

    private void publish(String title) {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle(title);
        article.setSlug(title.toLowerCase().replace(' ', '-'));
        article.setSummary("A summary.");
        article.setBody("Text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
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
