package in.ac.iitm.guide.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import in.ac.iitm.guide.MutableClock;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-021 and FR-022 through the pages: a reader flags an article with a message, the moderator sees
 * it in the inbox and closes it. Committed rows and the real security chain, so the CSRF token and
 * the moderator's gate are part of what is tested.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportFlowTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @TestConfiguration
    static class Clocks {
        @Bean
        MutableClock clock() {
            return new MutableClock();
        }
    }

    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
        // Three, so the limit is reached in a few requests; the stand's is ten (application.yml).
        registry.add("guide.report.limit.requests", () -> "3");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM report");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        // Each test starts a fresh hour, so one test's reports are not another's limit.
        clock.advance(Duration.ofHours(2));
    }

    @Test
    // trace:FR-021
    void the_article_page_offers_to_report_it() throws Exception {
        publish("Registering with FRRO");

        var page = mockMvc.perform(get("/articles/registering-with-frro"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page).contains("href=\"/articles/registering-with-frro/report\"");
    }

    @Test
    // trace:FR-021
    void a_reader_flags_an_article_with_a_message_and_the_moderator_sees_both_in_the_inbox() throws Exception {
        publish("Registering with FRRO");

        var result = report("registering-with-frro", "The office moved to the second floor.", "10.0.0.1");

        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/articles/registering-with-frro");
        assertThat(inbox()).contains("Registering with FRRO").contains("The office moved to the second floor.");
    }

    @Test
    // trace:FR-021
    void after_reporting_the_article_page_thanks_the_reader() throws Exception {
        publish("Registering with FRRO");
        var result = report("registering-with-frro", "Out of date.", "10.0.0.2");

        var page = mockMvc.perform(get("/articles/registering-with-frro").flashAttrs(result.getFlashMap()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page).contains("Thanks — OGE will look at it.");
    }

    @Test
    // trace:FR-021
    void a_report_without_a_message_is_refused_and_nothing_is_stored() throws Exception {
        publish("Registering with FRRO");

        var result = report("registering-with-frro", "   ", "10.0.0.3");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString()).contains("Say what is wrong");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM report", Long.class))
                .isZero();
    }

    @Test
    // trace:FR-021
    void a_message_over_two_thousand_characters_is_refused() throws Exception {
        publish("Registering with FRRO");

        var result = report("registering-with-frro", "x".repeat(2001), "10.0.0.4");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM report", Long.class))
                .isZero();
    }

    @Test
    // trace:FR-021
    void an_article_that_is_not_published_cannot_be_reported() throws Exception {
        assertThat(mockMvc.perform(get("/articles/no-such-article/report"))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(404);
    }

    @Test
    // trace:FR-021
    void past_the_limit_one_address_is_told_to_wait_and_its_report_is_not_stored() throws Exception {
        publish("Registering with FRRO");
        for (var i = 0; i < 3; i++) {
            report("registering-with-frro", "Report " + i, "10.0.0.5");
        }

        var result = report("registering-with-frro", "One more.", "10.0.0.5");

        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(result.getResponse().getHeader("Retry-After")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM report", Long.class))
                .isEqualTo(3);
    }

    @Test
    // trace:FR-022
    void a_closed_report_leaves_the_inbox() throws Exception {
        publish("Registering with FRRO");
        report("registering-with-frro", "The office moved.", "10.0.0.6");
        var session = loggedIn();
        var inbox = mockMvc.perform(get("/moderate/reports").session(session)).andReturn();
        var id = jdbc.queryForObject("SELECT id FROM report", UUID.class);

        var closed = send(session, inbox, "/moderate/reports/" + id + "/close", null);

        assertThat(closed.getResponse().getRedirectedUrl()).isEqualTo("/moderate/reports");
        assertThat(mockMvc.perform(get("/moderate/reports").session(session).flashAttrs(closed.getFlashMap()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .contains("Report closed")
                .doesNotContain("The office moved.");
    }

    @Test
    // trace:FR-022
    void the_inbox_and_closing_are_the_moderators_only() throws Exception {
        var inbox = mockMvc.perform(get("/moderate/reports")).andReturn();

        assertThat(inbox.getResponse().getRedirectedUrl()).endsWith("/moderate/login");
    }

    @Test
    // trace:FR-021
    void a_report_on_an_article_removed_since_is_not_in_the_inbox() throws Exception {
        publish("Registering with FRRO");
        report("registering-with-frro", "The office moved.", "10.0.0.7");
        jdbc.update("UPDATE article SET removed_at = CURRENT_TIMESTAMP");

        assertThat(inbox()).doesNotContain("The office moved.").contains("No reports");
    }

    private MvcResult report(String address, String message, String client) throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(
                        get("/articles/" + address + "/report").session(session).with(request -> {
                            request.setRemoteAddr(client);
                            return request;
                        }))
                .andReturn();
        return send(session, form, "/articles/" + address + "/reports", message, client);
    }

    private String inbox() throws Exception {
        return mockMvc.perform(get("/moderate/reports").session(loggedIn()))
                .andReturn()
                .getResponse()
                .getContentAsString();
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

    private MvcResult send(MockHttpSession session, MvcResult form, String action, String message) throws Exception {
        return send(session, form, action, message, "127.0.0.1");
    }

    private MvcResult send(MockHttpSession session, MvcResult form, String action, String message, String client)
            throws Exception {
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the page carries a CSRF token").isTrue();
        var request = post(action).session(session).param("_csrf", matcher.group(1));
        if (message != null) {
            request.param("message", message);
        }
        request.with(r -> {
            r.setRemoteAddr(client);
            return r;
        });
        // The CSRF token is kept in a cookie as well as the session (FEAT-017); send it back.
        if (form.getResponse().getCookies().length > 0) {
            request.cookie(form.getResponse().getCookies());
        }
        return mockMvc.perform(request).andReturn();
    }

    private void publish(String title) {
        transaction.executeWithoutResult(status -> {
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody("Text.");
            article.setPublishedAt(OffsetDateTime.now(clock));
            article.setUpdatedAt(OffsetDateTime.now(clock));
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }
}
