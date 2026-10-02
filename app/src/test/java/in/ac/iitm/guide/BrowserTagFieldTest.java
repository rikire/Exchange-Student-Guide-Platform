package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
 * Fix 3.6's tag field in a real browser, under the strict Content-Security-Policy: Tom Select over
 * the form's tag list (ADR-0022). A word becomes a chip on Enter or a comma, × removes it, a stored
 * tag is suggested while typing, and the form still posts one {@code tags} field per chip. Every test
 * also fails on a policy violation or a script error. Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserTagFieldTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private Tags tags;

    @Autowired
    private JdbcTemplate jdbc;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private final List<String> problems = new ArrayList<>();

    @BeforeAll
    void startTheBrowserAndPublishATaggedArticle() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle("Registering with FRRO");
            article.setSlug(ArticleAddress.slugOf(article.getTitle()).orElseThrow());
            article.setSummary("Register within 14 days of arriving in India.");
            article.setBody("Bring your passport.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(new LinkedHashSet<>(tags.named(List.of("visa", "arrival"))));
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
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @BeforeEach
    void openAProfile() {
        problems.clear();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
    }

    @AfterEach
    void closeTheProfileAndReportWhatWentWrong() {
        context.close();
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        assertThat(problems).as("policy violations and script errors").isEmpty();
    }

    @Test
    // trace:FR-010
    void a_word_becomes_a_chip_on_enter_and_on_a_comma() {
        var page = openTheForm();

        page.click(".ts-control");
        page.keyboard().type("hostel");
        page.keyboard().press("Enter");
        page.keyboard().type("mess food,");

        assertThat(chips(page)).containsExactly("hostel", "mess food");
    }

    @Test
    // trace:FR-010
    void the_cross_on_a_chip_removes_it() {
        var page = openTheForm();
        page.click(".ts-control");
        page.keyboard().type("hostel,mess food,");

        page.click(".ts-control .item[data-value='hostel'] .remove");

        assertThat(chips(page)).containsExactly("mess food");
    }

    @Test
    // trace:FR-010
    void a_stored_tag_is_suggested_while_its_start_is_typed() {
        var page = openTheForm();

        page.click(".ts-control");
        page.keyboard().type("vi");

        page.waitForSelector(".ts-dropdown .option[data-value='visa']");
        page.click(".ts-dropdown .option[data-value='visa']");
        assertThat(chips(page)).containsExactly("visa");
    }

    @Test
    // trace:FR-010
    void the_chips_are_stored_as_the_submissions_tags() {
        var page = openTheForm();
        page.fill("input[name=title]", "Hostel rooms");
        page.fill("input[name=summary]", "A summary.");
        page.evaluate("() => document.querySelector('.CodeMirror').CodeMirror.setValue('Rooms are shared.')");

        page.click(".ts-control");
        page.keyboard().type("vi");
        page.click(".ts-dropdown .option[data-value='visa']");
        page.keyboard().type("Hostel,");
        page.click("button[type=submit]");
        page.waitForURL("**/confirmation");

        assertThat(jdbc.queryForList(
                        "SELECT t.name FROM submission_tag st JOIN tag t ON t.id = st.tag_id", String.class))
                .containsExactlyInAnyOrder("visa", "hostel");
    }

    private Page openTheForm() {
        var page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                problems.add(message.text());
            }
        });
        page.onPageError(error -> problems.add(error));
        page.navigate("http://localhost:" + port + "/submit");
        page.waitForSelector(".ts-wrapper");
        return page;
    }

    @SuppressWarnings("unchecked")
    private static List<String> chips(Page page) {
        return (List<String>) page.evaluate(
                "() => [...document.querySelectorAll('.ts-control .item')].map(item => item.dataset.value)");
    }
}
