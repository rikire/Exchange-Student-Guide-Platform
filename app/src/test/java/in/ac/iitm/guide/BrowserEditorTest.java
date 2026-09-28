package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
 * FR-027's editor in a real browser, under the strict Content-Security-Policy (ADR-0013): the
 * formatting controls, the server's preview beside the text, and the draft kept in the browser. Every
 * test also fails on a policy violation or a script error. Width and WCAG are BrowserLayoutTest's,
 * which measures the form with the editor on it. Runs only under {@code -P browser}.
 *
 * <p>Typing here goes through the browser's text input, not an operating system's input method, so
 * the phone check of ADR-0013 (Gboard and the iOS keyboard, in Hindi and Tamil) is still a person's.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserEditorTest {

    private static final String EVERY_SCRIPT = "छात्रावास में पंजीकरण। விடுதி பதிவு. é 👋🏽";

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
            article.setBody("Bring your passport.");
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
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article");
    }

    /** A fresh browser profile per test, so no draft carries over from one test to the next. */
    @BeforeEach
    void openAProfile() {
        problems.clear();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
    }

    @AfterEach
    void closeTheProfileAndReportWhatWentWrong() {
        context.close();
        assertThat(problems).as("policy violations and script errors").isEmpty();
    }

    @Test
    // trace:FR-027
    void a_formatting_control_used_with_text_selected_puts_the_markdown_around_the_selection() {
        var page = openTheForm();
        page.evaluate("() => { const cm = document.querySelector('.CodeMirror').CodeMirror;"
                + " cm.setValue('Bring your passport'); cm.setSelection({line: 0, ch: 11}, {line: 0, ch: 19}); }");

        page.click(".editor-toolbar button.bold");

        assertThat(editorText(page)).isEqualTo("Bring your **passport**");
    }

    @Test
    // trace:FR-027
    void the_preview_beside_the_text_shows_the_servers_rendering_with_wiki_links_resolved() {
        var page = openTheForm();

        page.click(".CodeMirror");
        page.keyboard().type("See [[registering with frro]] and [[Nowhere Yet]].");

        page.waitForSelector(".editor-preview-side a.wikilink[href='/articles/registering-with-frro']");
        page.waitForSelector(".editor-preview-side .wikilink-missing");
        var editor = page.locator(".CodeMirror").boundingBox();
        var preview = page.locator(".editor-preview-side").boundingBox();
        assertThat(preview.x)
                .as("the preview stands to the right of the text")
                .isGreaterThanOrEqualTo(editor.x + editor.width);
    }

    @Test
    // trace:FR-027
    void text_in_any_script_typed_into_the_editor_is_previewed_and_submitted_unchanged() {
        var page = openTheForm();

        page.click(".CodeMirror");
        page.keyboard().type(EVERY_SCRIPT);
        assertThat(editorText(page)).as("the text in the editor as typed").isEqualTo(EVERY_SCRIPT);
        page.waitForFunction(
                "text => document.querySelector('.editor-preview-side').textContent.includes(text)", EVERY_SCRIPT);
        fillAndSubmit(page, "In every script");

        assertThat(storedBody()).isEqualTo(EVERY_SCRIPT);
    }

    @Test
    // trace:FR-027
    void an_unsent_draft_is_restored_when_the_form_is_reopened_and_cleared_once_the_submission_succeeds() {
        var page = openTheForm();
        page.click(".CodeMirror");
        page.keyboard().type("A draft about the hostel.");
        page.waitForFunction("() => Object.keys(localStorage).some(key => key.startsWith('smde_'))");
        page.close();

        var reopened = openTheForm();
        assertThat(editorText(reopened)).as("the draft after reopening").isEqualTo("A draft about the hostel.");

        fillAndSubmit(reopened, "A draft that was sent");
        var again = openTheForm();
        assertThat(editorText(again))
                .as("the form after the submission succeeded")
                .isEmpty();
        assertThat((List<?>) again.evaluate("() => Object.keys(localStorage).filter(key => key.startsWith('smde_'))"))
                .as("drafts left in the browser")
                .isEmpty();
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
        page.waitForSelector(".EasyMDEContainer .editor-preview-active-side");
        return page;
    }

    private static String editorText(Page page) {
        return (String) page.evaluate("() => document.querySelector('.CodeMirror').CodeMirror.getValue()");
    }

    private static void fillAndSubmit(Page page, String title) {
        page.fill("input[name=title]", title);
        page.fill("input[name=summary]", "A summary.");
        page.click("button[type=submit]");
        page.waitForURL("**/confirmation");
    }

    private String storedBody() {
        return jdbc.queryForObject("SELECT body FROM submission WHERE title = 'In every script'", String.class);
    }
}
