package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
 * NFR-007's keyboard half, over the demo scenario (phase 3's goal): search, read, follow a wiki link,
 * propose an edit in the editor, and the moderator approves it — with Tab, typing and Enter only,
 * never a click. Every element Tab reaches must show where the focus is, and the editor must let the
 * focus leave it (WCAG 2.1.1, 2.1.2, 2.4.7). The names and roles a screen reader announces are axe's,
 * in BrowserLayoutTest. Runs only under {@code -P browser}.
 */
// trace:NFR-007
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserKeyboardTest {

    private static final String PASSWORD = "the office's password";

    /** More presses than any page of the scenario has stops before its target. */
    private static final int MOST_TABS = 60;

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

    @BeforeAll
    void startTheBrowserAndPublishTwoLinkedArticles() {
        playwright = Playwright.create();
        browser = TestBrowser.launch(playwright);
        publish("Registering with FRRO", "FRRO registration is due within 14 days. Bring your [[Hostel Life]] papers.");
        publish("Hostel Life", "Rooms are shared.");
    }

    @AfterAll
    void closeTheBrowserAndClearTheDatabaseAndTheIndex() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        jdbc.execute("DELETE FROM revision");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        // The rows went by JDBC, which the index does not see.
        transaction.executeWithoutResult(
                status -> Search.session(entityManager).workspace().purge());
    }

    @Test
    // trace:NFR-007
    void the_demo_scenario_can_be_done_from_the_keyboard_alone_with_the_focus_always_visible() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900))) {
            var page = context.newPage();
            page.navigate(url("/"));

            tabTo(page, "input[name=q]");
            page.keyboard().type("FRRO registration");
            page.keyboard().press("Enter");
            page.waitForURL("**/search?q=*");

            tabTo(page, "a[href='/articles/registering-with-frro']");
            page.keyboard().press("Enter");
            page.waitForURL("**/articles/registering-with-frro");

            tabTo(page, "a.wikilink[href='/articles/hostel-life']");
            page.keyboard().press("Enter");
            page.waitForURL("**/articles/hostel-life");

            tabTo(page, "a[href='/articles/hostel-life/edit']");
            page.keyboard().press("Enter");
            page.waitForURL("**/articles/hostel-life/edit");
            page.waitForSelector(".EasyMDEContainer");

            tabTo(page, ".CodeMirror textarea, .CodeMirror [contenteditable=true]");
            page.keyboard().press("ControlOrMeta+End");
            page.keyboard().type(" Laundry is on the ground floor.");
            tabTo(page, "button[type=submit]");
            page.keyboard().press("Enter");
            page.waitForURL("**/confirmation");

            page.navigate(url("/moderate/login"));
            tabTo(page, "input[name=password]");
            page.keyboard().type(PASSWORD);
            page.keyboard().press("Enter");
            page.waitForURL("**/moderate/queue");

            tabTo(page, "a[href^='/moderate/submissions/']");
            page.keyboard().press("Enter");
            page.waitForURL("**/moderate/submissions/*");

            tabTo(page, "form[action$='/approve'] button[type=submit]");
            page.keyboard().press("Enter");
            page.waitForURL("**/moderate/queue");
        }

        assertThat(jdbc.queryForObject("SELECT body FROM article WHERE slug = 'hostel-life'", String.class))
                .as("the edit proposed and approved from the keyboard is published")
                .isEqualTo("Rooms are shared. Laundry is on the ground floor.");
    }

    /**
     * Presses Tab until the focus is on an element matching {@code selector}, checking at every stop
     * that the focus can be seen. Fails with the stops it made when the target is never reached, which
     * is also how a keyboard trap shows: the same element over and over.
     */
    private static void tabTo(Page page, String selector) {
        var stops = new ArrayList<String>();
        for (int press = 0; press < MOST_TABS; press++) {
            // A field with autofocus is already where the keyboard needs to be.
            if (Boolean.TRUE.equals(page.evaluate("s => document.activeElement.matches(s)", selector))) {
                return;
            }
            page.keyboard().press("Tab");
            var stop = (String) page.evaluate(DESCRIBE_FOCUS);
            stops.add(stop);
            // Past the last element the focus leaves for the browser's own controls: nothing to draw.
            if (!stop.startsWith("<body") && !Boolean.TRUE.equals(page.evaluate(FOCUS_VISIBLE))) {
                fail("the focus is not visible on " + stop + " at " + page.url());
            }
        }
        fail("Tab never reached " + selector + " at " + page.url() + "; it stopped at " + lastOf(stops));
    }

    private static List<String> lastOf(List<String> stops) {
        return stops.subList(Math.max(0, stops.size() - 8), stops.size());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void publish(String title, String body) {
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody(body);
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }

    private static final String DESCRIBE_FOCUS =
            """
            () => {
              const e = document.activeElement;
              return '<' + e.tagName.toLowerCase() + (e.name ? ' name=' + e.name : '')
                + (e.getAttribute('href') ? ' href=' + e.getAttribute('href') : '') + '> "'
                + (e.textContent || '').trim().slice(0, 30) + '"';
            }
            """;

    /**
     * An outline or a box shadow drawn on the focused element: the browser's ring or our own. In the
     * editor the focus is on CodeMirror's hidden input, so the ring has to be on the editor itself.
     */
    private static final String FOCUS_VISIBLE =
            """
            () => {
              const e = document.activeElement;
              const style = getComputedStyle(e.closest('.CodeMirror') || e.closest('.ts-control') || e);
              const outline = style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0;
              return outline || style.boxShadow !== 'none';
            }
            """;
}
