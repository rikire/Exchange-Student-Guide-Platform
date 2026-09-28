package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Media;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-030 in a real browser, under the Content-Security-Policy: the "Save as PDF" control opens the
 * browser's print, and the printed page is the article without the site around it. Every test also
 * fails on a policy violation or a script error. Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserPrintTest {

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
    void startTheBrowserAndPublishAnArticle() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle("Registering with FRRO");
            article.setSlug(ArticleAddress.slugOf(article.getTitle()).orElseThrow());
            article.setSummary("Register within 14 days of arriving in India.");
            article.setBody("Bring your passport. छात्रावास में पंजीकरण। விடுதி பதிவு.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(Set.of());
            entityManager.persist(article);
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
        jdbc.execute("DELETE FROM article");
    }

    @BeforeEach
    void openAProfile() {
        problems.clear();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
        // The print dialog cannot be driven from a test; replacing window.print records that it was asked for.
        context.addInitScript("window.print = () => { window.__printed = true; };");
    }

    @AfterEach
    void closeTheProfileAndReportWhatWentWrong() {
        context.close();
        assertThat(problems).as("policy violations and script errors").isEmpty();
    }

    @Test
    // trace:FR-030
    void save_as_pdf_is_shown_and_opens_the_browsers_print() {
        var page = openTheArticle();

        page.click("button[data-print]");

        assertThat(page.evaluate("() => window.__printed === true")).isEqualTo(true);
    }

    @Test
    // trace:FR-030
    void the_printed_page_keeps_the_article_and_leaves_out_the_site_around_it() {
        var page = openTheArticle();

        page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));

        for (var shown : List.of(".article-title", ".article-body")) {
            assertThat(page.isVisible(shown)).as("%s is printed", shown).isTrue();
        }
        for (var hidden : List.of(".site-header", ".site-footer", ".article-actions", "button[data-print]")) {
            assertThat(page.isVisible(hidden)).as("%s is not printed", hidden).isFalse();
        }
    }

    private Page openTheArticle() {
        var page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                problems.add(message.text());
            }
        });
        page.onPageError(error -> problems.add(error));
        page.navigate("http://localhost:" + port + "/articles/registering-with-frro");
        return page;
    }
}
