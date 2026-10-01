package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR-0020 in a real browser: a reader in Moscow, with the clock stopped at 13:58 UTC on 1 Oct 2026,
 * sees times relative to now and, on hover, in Moscow time; never IST. Every test also fails on a
 * policy violation or a script error. The server-rendered fallback is MockMvc's to test. Runs only
 * under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserLocalTimeTest {

    private static final String PASSWORD = "the office's password";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-01T13:58:00Z");

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private final List<String> problems = new ArrayList<>();

    @BeforeAll
    void startTheBrowserAndArrangeTheGuide() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        transaction.executeWithoutResult(status -> {
            var visa = new Tag();
            visa.setName("visa");
            entityManager.persist(visa);
            entityManager.persist(article("Registering with FRRO", NOW.minusDays(3), visa));
            entityManager.persist(
                    article("Applying for your visa", OffsetDateTime.parse("2026-08-01T06:00:00Z"), visa));
            entityManager.persist(pending(NOW.minusHours(2)));
        });
    }

    @AfterAll
    void closeTheBrowserAndClearTheDatabase() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @BeforeEach
    void openAProfileInMoscow() {
        problems.clear();
        context = browser.newContext(
                new Browser.NewContextOptions().setViewportSize(1280, 900).setTimezoneId("Europe/Moscow"));
        context.clock().setFixedTime(NOW.toInstant().toEpochMilli());
    }

    @AfterEach
    void closeTheProfileAndReportWhatWentWrong() {
        context.close();
        assertThat(problems).as("policy violations and script errors").isEmpty();
    }

    @Test
    // trace:FR-014
    void the_queue_says_how_long_ago_a_submission_was_sent() {
        var page = signedIn("/moderate/queue");

        assertThat(shown(page, "relative-time")).isEqualTo("2 hours ago");
    }

    @Test
    // trace:FR-014
    void the_queue_gives_the_readers_own_time_on_hover() {
        var page = signedIn("/moderate/queue");

        assertThat(page.locator("relative-time").first().getAttribute("title"))
                .contains("1 Oct 2026")
                .contains("14:58")
                .doesNotContain("IST");
    }

    @Test
    // trace:FR-008
    void a_recent_update_reads_as_days_ago_and_an_old_one_as_a_date() {
        var page = open("/tags/visa");

        shown(page, ".article-card-meta relative-time");
        var dates = page.locator(".article-card-meta relative-time")
                .evaluateAll("els => els.map(el => el.shadowRoot.textContent)");

        assertThat(dates).isEqualTo(List.of("3 days ago", "on 1 Aug 2026"));
    }

    private static Article article(String title, OffsetDateTime updatedAt, Tag tag) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary of " + title + ".");
        article.setBody("Body.");
        article.setPublishedAt(updatedAt);
        article.setUpdatedAt(updatedAt);
        article.setTags(Set.of(tag));
        return article;
    }

    private static Submission pending(OffsetDateTime submittedAt) {
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-TIME-0000-0001");
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Getting a SIM card");
        submission.setSummary("Which shops sell one.");
        submission.setBody("Bring your passport.");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(submittedAt);
        submission.setTags(new HashSet<>());
        return submission;
    }

    /** The text a reader sees: the element draws it in its shadow root once its script has run. */
    private static String shown(Page page, String selector) {
        page.waitForFunction("sel => document.querySelector(sel)?.shadowRoot?.textContent", selector);
        return (String) page.locator(selector).first().evaluate("el => el.shadowRoot.textContent");
    }

    private Page signedIn(String path) {
        var page = context.newPage();
        watch(page);
        page.navigate("http://localhost:" + port + "/moderate/login");
        page.fill("input[name=password]", PASSWORD);
        page.click("button[type=submit]");
        page.waitForURL(url -> !url.endsWith("/moderate/login"));
        page.navigate("http://localhost:" + port + path);
        return page;
    }

    private Page open(String path) {
        var page = context.newPage();
        watch(page);
        page.navigate("http://localhost:" + port + path);
        return page;
    }

    private void watch(Page page) {
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                problems.add(message.text());
            }
        });
        page.onPageError(error -> problems.add(error));
    }
}
