package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * NFR-005's limits per client address: submissions (a new article and an edit counted together),
 * the editor's preview, and failed moderator logins (ADR-0009, ADR-0019). The limits are the
 * application's defaults, which the test settings raise for every other test class.
 *
 * <p>The counters live as long as the context, so every test sends from addresses of its own, and
 * the clock only moves forward.
 */
@SpringBootTest(
        properties = {
            "guide.contribute.submission-limit.requests=5",
            "guide.contribute.submission-limit.per=1h",
            "guide.contribute.preview-limit.requests=120",
            "guide.contribute.preview-limit.per=1m",
            "guide.admin.failed-login-limit.requests=10",
            "guide.admin.failed-login-limit.per=15m"
        })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(RateLimitTest.Clocks.class)
class RateLimitTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final String TOO_MANY = "Too many submissions from your network. Please try again in";

    @TestConfiguration
    static class Clocks {
        @Bean
        MutableClock clock() {
            return new MutableClock();
        }
    }

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
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
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:NFR-005
    // trace:FR-013
    void the_sixth_submission_in_an_hour_answers_429_on_the_form_with_the_text_kept() throws Exception {
        var address = "198.51.100.1";
        for (var i = 1; i <= 5; i++) {
            assertThat(submitNew(address, "Article " + i).getStatus())
                    .as("submission %d", i)
                    .isEqualTo(302);
        }

        var refused = submitNew(address, "Article six");

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(retryAfter(refused)).isBetween(1L, 3600L);
        assertThat(refused.getContentAsString())
                .contains(TOO_MANY)
                .contains("value=\"Article six\"")
                .contains("The text of Article six.");
        assertThat(submissionCount()).isEqualTo(5);
    }

    @Test
    // trace:NFR-005
    void new_articles_and_edits_share_one_count() throws Exception {
        var address = "198.51.100.2";
        publish("Hostel Life");
        for (var i = 1; i <= 3; i++) {
            assertThat(submitNew(address, "Article " + i).getStatus()).isEqualTo(302);
        }
        for (var i = 1; i <= 2; i++) {
            assertThat(submitEdit(address, "Hostel Life").getStatus()).isEqualTo(302);
        }

        var refused = submitEdit(address, "Hostel Life");

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getContentAsString()).contains(TOO_MANY).contains("The text of Hostel Life.");
    }

    @Test
    // trace:NFR-005
    void a_submission_refused_on_the_form_is_not_counted() throws Exception {
        var address = "198.51.100.3";
        publish("Hostel Life");
        for (var i = 1; i <= 6; i++) {
            assertThat(submitNew(address, "Hostel Life").getStatus()).isEqualTo(422);
        }

        for (var i = 1; i <= 5; i++) {
            assertThat(submitNew(address, "Article " + i).getStatus())
                    .as("submission %d", i)
                    .isEqualTo(302);
        }
    }

    @Test
    // trace:NFR-005
    void another_address_has_a_count_of_its_own() throws Exception {
        exhaustSubmissions("198.51.100.4");

        assertThat(submitNew("198.51.100.5", "Someone else").getStatus()).isEqualTo(302);
    }

    @Test
    // trace:NFR-005
    void a_forwarded_for_header_does_not_change_whose_count_it_is() throws Exception {
        var address = "198.51.100.6";
        for (var i = 1; i <= 5; i++) {
            var sent = submitNew(address, "Article " + i, request -> request.header("X-Forwarded-For", "203.0.113.9"));
            assertThat(sent.getStatus()).isEqualTo(302);
        }

        var another = submitNew(address, "Article six", request -> request.header("X-Forwarded-For", "203.0.113.10"));

        assertThat(another.getStatus()).as("a new header is not a new address").isEqualTo(429);
    }

    @Test
    // trace:NFR-005
    // trace:FR-013
    void the_count_starts_again_once_the_hour_has_passed() throws Exception {
        var address = "198.51.100.7";
        exhaustSubmissions(address);

        clock.advance(Duration.ofHours(1));

        assertThat(submitNew(address, "An hour later").getStatus()).isEqualTo(302);
    }

    @Test
    // trace:NFR-005
    void the_preview_past_its_limit_answers_429_with_a_fragment_for_the_pane() throws Exception {
        var address = "198.51.100.8";
        var form = mockMvc.perform(get("/submit")).andReturn();
        for (var i = 1; i <= 120; i++) {
            assertThat(preview(form, address).getStatus()).as("preview %d", i).isEqualTo(200);
        }

        var refused = preview(form, address);

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(retryAfter(refused)).isBetween(1L, 60L);
        assertThat(refused.getContentAsString())
                .startsWith("<p>")
                .contains("Too many")
                .doesNotContain("<html");
    }

    @Test
    // trace:NFR-005
    void after_ten_failed_logins_even_the_right_password_answers_429() throws Exception {
        var address = "198.51.100.9";
        for (var i = 1; i <= 10; i++) {
            assertThat(logIn(address, "not the password").getResponse().getStatus())
                    .as("attempt %d", i)
                    .isEqualTo(401);
        }

        var refused = logIn(address, PASSWORD);

        assertThat(refused.getResponse().getStatus()).isEqualTo(429);
        assertThat(retryAfter(refused.getResponse())).isBetween(1L, 900L);
        assertThat(refused.getResponse().getContentAsString())
                .contains("Too many failed attempts")
                .contains("name=\"password\"");
        assertThat(mockMvc.perform(get("/moderate/queue")
                                .session((MockHttpSession) refused.getRequest().getSession()))
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl())
                .as("the refused attempt opens nothing")
                .endsWith("/moderate/login");
    }

    @Test
    // trace:NFR-005
    void logins_with_the_right_password_are_not_counted() throws Exception {
        var address = "198.51.100.10";
        for (var i = 1; i <= 12; i++) {
            assertThat(logIn(address, PASSWORD).getResponse().getStatus()).isEqualTo(302);
        }

        for (var i = 1; i <= 10; i++) {
            assertThat(logIn(address, "not the password").getResponse().getStatus())
                    .as("failure %d", i)
                    .isEqualTo(401);
        }
    }

    @Test
    // trace:NFR-005
    void a_limit_reached_is_logged_as_a_warning_without_the_address(CapturedOutput output) throws Exception {
        exhaustSubmissions("198.51.100.211");
        for (var i = 1; i <= 11; i++) {
            logIn("198.51.100.212", "not the password");
        }

        assertThat(output)
                .contains("Submission rate limit reached")
                .contains("Moderator login rate limit reached")
                .doesNotContain("198.51.100.211")
                .doesNotContain("198.51.100.212");
    }

    private void exhaustSubmissions(String address) throws Exception {
        for (var i = 1; i <= 5; i++) {
            assertThat(submitNew(address, "Article " + i).getStatus()).isEqualTo(302);
        }
        assertThat(submitNew(address, "One too many").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse submitNew(String address, String title) throws Exception {
        return submitNew(address, title, request -> request);
    }

    private MockHttpServletResponse submitNew(
            String address, String title, UnaryOperator<MockHttpServletRequestBuilder> extra) throws Exception {
        return submit("/submit", "/submissions", address, title, extra);
    }

    private MockHttpServletResponse submitEdit(String address, String title) throws Exception {
        var slug = ArticleAddress.slugOf(title).orElseThrow();
        return submit("/articles/" + slug + "/edit", "/articles/" + slug + "/edits", address, title, r -> r);
    }

    /** The form first, for its token and cookie, then the post from {@code address}, as a browser sends it. */
    private MockHttpServletResponse submit(
            String formPath,
            String action,
            String address,
            String title,
            UnaryOperator<MockHttpServletRequestBuilder> extra)
            throws Exception {
        var form = mockMvc.perform(get(formPath)).andReturn();
        var request = post(action)
                .with(from(address))
                .session((MockHttpSession) form.getRequest().getSession())
                .param("_csrf", token(form))
                .param("title", title)
                .param("summary", "A summary.")
                .param("body", "The text of " + title + ".");
        return mockMvc.perform(extra.apply(withCookiesOf(form, request)))
                .andReturn()
                .getResponse();
    }

    private MockHttpServletResponse preview(MvcResult form, String address) throws Exception {
        var request = post("/contribute/preview")
                .with(from(address))
                .param("_csrf", token(form))
                .param("body", "**Bold**");
        return mockMvc.perform(withCookiesOf(form, request)).andReturn().getResponse();
    }

    private MvcResult logIn(String address, String password) throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var request = post("/moderate/login")
                .with(from(address))
                .session(session)
                .param("_csrf", token(form))
                .param("password", password);
        return mockMvc.perform(withCookiesOf(form, request)).andReturn();
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static long retryAfter(MockHttpServletResponse response) {
        var header = response.getHeader("Retry-After");
        assertThat(header).as("Retry-After").isNotNull();
        return Long.parseLong(header);
    }

    private static String token(MvcResult page) throws Exception {
        var matcher = CSRF.matcher(page.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the page carries a CSRF token").isTrue();
        return matcher.group(1);
    }

    private static MockHttpServletRequestBuilder withCookiesOf(MvcResult form, MockHttpServletRequestBuilder request) {
        var cookies = form.getResponse().getCookies();
        return cookies.length == 0 ? request : request.cookie(cookies);
    }

    private int submissionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM submission", Integer.class);
    }

    private void publish(String title) {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody("The original text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(Set.of());
        transaction.executeWithoutResult(status -> entityManager.persist(article));
    }
}
