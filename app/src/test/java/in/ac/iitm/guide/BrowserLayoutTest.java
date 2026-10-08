package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaTestFiles;
import in.ac.iitm.guide.media.Upload;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.yaml.snakeyaml.Yaml;

/**
 * NFR-008 and NFR-007's automated half, in a real browser (ADR-0014): every built GET route of
 * routes.yml, and the not-found page, at four widths. Runs only under {@code -P browser}.
 *
 * <p>The pages come from the route contract rather than a list here, so a route marked built is
 * checked from that moment; a path variable it uses must have a sample below, or this fails. The
 * moderator's pages are measured signed in: without a session they redirect to the login page, and
 * this measured that page in their place until 28 Sep.
 */
// trace:NFR-007
// trace:NFR-008
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserLayoutTest {

    private static final List<Integer> WIDTHS = List.of(320, 768, 1280, 1920);
    private static final Set<Integer> PHONE_AND_TABLET = Set.of(320, 768);
    // Each group led by a digit and filled with the widest letters (walkthrough-fixes 3.9, D1): a browser
    // does not break a line between a hyphen and a digit, so SUB-1872-42CQ-02KQ ran off a 320 px phone.
    private static final String NUMBER = "SUB-0WMW-0MWM-0WMW";
    private static final String PASSWORD = "the office's password";

    /**
     * Fix 1.8 (F-31): the longest title the guide takes, with no space to break at, and a summary at
     * its limit with a long unbroken word in it. Shown on every list, so no page can scroll sideways
     * because of what a contributor typed.
     */
    private static final String LONG_TITLE =
            "Registeringwiththeforeignersregionalregistrationoffice".repeat(5).substring(0, 255);

    private static final String LONG_SUMMARY = ("Bring "
                    + "passportvisaadmissionletterphotographsandproofofaddresstotheofficeonthefirstfloor".repeat(2)
                    + " and wait for your number to be called at the counter.")
            .substring(0, 220);

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

    @Autowired
    private MediaAssets media;

    private Playwright playwright;
    private Browser browser;

    @BeforeAll
    void startTheBrowserAndWriteTheFixtures() {
        playwright = Playwright.create();
        browser = TestBrowser.launch(playwright);
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle("Registering with FRRO");
            article.setSlug(ArticleAddress.slugOf(article.getTitle()).orElseThrow());
            article.setSummary("Register within 14 days of arriving in India.");
            article.setBody("## Before you go\n\nBring your passport, visa and [[Hostel Life]] papers.\n\n"
                    + "- Photographs\n- Proof of address\n\nछात्रावास में पंजीकरण। விடுதி பதிவு.\n\n"
                    + "| Semester | Start date | End date | Where to register before the semester begins |\n"
                    + "|---|---|---|---|\n"
                    + "| Semester 1 | 15.01.2026 | 31.05.2026 | The Office of Global Engagement, first floor |\n"
                    + "| Semester 2 | 27.07.2026 | 30.11.2026 | The Office of Global Engagement, first floor |");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            var tag = new Tag();
            tag.setName("visa");
            entityManager.persist(tag);
            article.setTags(Set.of(tag));
            entityManager.persist(article);
        });
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle(LONG_TITLE);
            article.setSlug(ArticleAddress.slugOf(LONG_TITLE).orElseThrow());
            article.setSummary(LONG_SUMMARY);
            article.setBody("All about FRRO, with a title no one should write.");
            article.setPublishedAt(now.minusDays(1));
            article.setUpdatedAt(now.minusDays(1));
            article.setTags(Set.of(entityManager
                    .createQuery("SELECT t FROM Tag t WHERE t.name = 'visa'", Tag.class)
                    .getSingleResult()));
            entityManager.persist(article);
        });
        jdbc.update(
                "INSERT INTO submission (id, submission_number, type, title, summary, body, status, submitted_at)"
                        + " VALUES (?, 'SUB-LONG-TITL-E000', 'NEW_ARTICLE', ?, ?, 'b', 'PENDING', CURRENT_TIMESTAMP)",
                UUID.randomUUID(),
                LONG_TITLE,
                LONG_SUMMARY);
        attachToTheArticle("form.jpg", MediaTestFiles.jpeg(1600, 1200));
        attachToTheArticle(
                "FRRO checklist with a long file name for a narrow phone screen.pdf", MediaTestFiles.pdf(2_000));
        // An edit, so the review is measured with FR-029's comparison: a changed word in a long line,
        // a new paragraph and a changed title and summary.
        jdbc.update(
                "INSERT INTO submission (id, submission_number, type, target_article_id, title, summary, body,"
                        + " status, submitted_at) VALUES (?, ?, 'EDIT', ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)",
                UUID.randomUUID(),
                NUMBER,
                registering(),
                "Registering with the FRRO online",
                "Register on the e-FRRO portal within 14 days of arriving in India.",
                "## Before you go\n\nBring your passport, visa, admission letter and [[Hostel Life]] papers.\n\n"
                        + "- Photographs\n- Proof of address\n\nछात्रावास में पंजीकरण। விடுதி பதிவு.\n\n"
                        + "Registration-is-done-through-the-e-FRRO-portal-and-needs-no-visit-in-most-cases.");
    }

    /** A wide photo and a long-named document, so the article is measured with its media (FEAT-009). */
    private void attachToTheArticle(String name, byte[] bytes) {
        var submission = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO submission (id, submission_number, type, title, summary, body, status, submitted_at)"
                        + " VALUES (?, ?, 'NEW_ARTICLE', 't', 's', 'b', 'APPROVED', CURRENT_TIMESTAMP)",
                submission,
                "SUB-MEDI-A000-%04d".formatted(++attached));
        media.attach(submission, new Upload(name, bytes.length, new ByteArrayResource(bytes)));
        media.moveToArticle(submission, registering());
    }

    private UUID registering() {
        return jdbc.queryForObject("SELECT id FROM article WHERE slug = 'registering-with-frro'", UUID.class);
    }

    private int attached;

    @AfterAll
    void closeTheBrowserAndClearTheFixtures() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @TestFactory
    Stream<DynamicTest> every_built_page_fits_every_width_and_passes_wcag_aa() throws IOException {
        var tests = new ArrayList<DynamicTest>();
        for (var path : pages()) {
            for (var width : WIDTHS) {
                tests.add(DynamicTest.dynamicTest(path + " at " + width + " px", () -> check(path, width)));
            }
        }
        return tests.stream();
    }

    private void check(String path, int width) {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(width, 900))) {
            Page page = context.newPage();
            if (path.startsWith("/moderate/") && !path.equals("/moderate/login")) {
                signIn(page);
            }
            page.navigate("http://localhost:" + port + path);
            assertThat(page.url())
                    .as("the page measured is the one asked for, not one it redirected to")
                    .isEqualTo("http://localhost:" + port + path);

            var problems = new ArrayList<String>();
            var scrollWidth = ((Number) page.evaluate("document.documentElement.scrollWidth")).intValue();
            if (scrollWidth > width) {
                problems.add("scrolls sideways: the page is " + scrollWidth + " px wide, past the edge: "
                        + page.evaluate(PAST_THE_EDGE));
            }
            if (PHONE_AND_TABLET.contains(width)) {
                problems.addAll(stringList(page.evaluate(SMALL_TEXT)));
                problems.addAll(stringList(page.evaluate(SMALL_TARGETS)));
            }
            if (width >= 1280) {
                // Fix 3.1 (F-17): the page frame uses the width of a wide window.
                var mainWidth = ((Number) page.evaluate("document.querySelector('main').getBoundingClientRect().width"))
                        .doubleValue();
                if (mainWidth < 0.6 * width) {
                    problems.add("the main content is " + Math.round(mainWidth) + " px, under 60 % of the window");
                }
            }
            if (width == 1280 && !Boolean.TRUE.equals(page.evaluate(FONT_LOADED))) {
                problems.add("the Noto Sans typeface did not load");
            }
            if (width == 320 || width == 1280) {
                var results = new AxeBuilder(page)
                        .withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"))
                        .analyze();
                results.getViolations()
                        .forEach(violation -> problems.add("axe " + violation.getId() + ": " + violation.getHelp()
                                + " (" + violation.getNodes().size() + " elements)"));
            }

            assertThat(problems).as("%s at %d px", path, width).isEmpty();
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_phone_the_header_links_open_from_a_menu_each_a_44_px_target() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/");
            var links = page.locator(".site-menu a");
            assertThat(links.first().isVisible())
                    .as("the links are folded away at first")
                    .isFalse();

            page.click(".site-menu summary");

            assertThat(links.count()).isEqualTo(6);
            for (var i = 0; i < links.count(); i++) {
                assertThat(links.nth(i).isVisible()).isTrue();
                assertThat(links.nth(i).boundingBox().height).isGreaterThanOrEqualTo(44);
            }
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_phone_the_menu_sits_at_the_right_and_opens_inside_the_screen() {
        for (var width : List.of(320, 390)) {
            try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(width, 844))) {
                var page = context.newPage();
                page.navigate("http://localhost:" + port + "/");
                var menu = page.locator(".site-menu summary").boundingBox();

                page.click(".site-menu summary");
                var list = page.locator(".site-menu ul").boundingBox();

                assertThat(menu.x + menu.width)
                        .as("the button at the right edge, %d px", width)
                        .isGreaterThan(width - 24);
                assertThat(list.x)
                        .as("the list starts on the screen, %d px", width)
                        .isGreaterThanOrEqualTo(0);
                assertThat(list.x + list.width).as("and ends on it").isLessThanOrEqualTo(width);
            }
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_wide_screen_the_header_links_are_shown_without_a_menu() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/");

            assertThat(page.locator(".site-nav > .site-links a").count()).isEqualTo(6);
            assertThat(page.locator(".site-nav > .site-links a").first().isVisible())
                    .isTrue();
            assertThat(page.locator(".site-menu").isVisible()).isFalse();
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void the_queue_keeps_every_review_link_in_view_beside_a_long_title() {
        for (var width : WIDTHS) {
            try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(width, 900))) {
                var page = context.newPage();
                signIn(page);
                page.navigate("http://localhost:" + port + "/moderate/queue");

                var outside = page.evaluate("() => [...document.querySelectorAll('.queue-table a')]"
                        + ".filter(a => a.getBoundingClientRect().right > window.innerWidth + 1).length");

                assertThat(((Number) outside).intValue())
                        .as("review links past the edge at %d px", width)
                        .isZero();
            }
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_wide_screen_the_moderators_tables_fill_the_frame() {
        // Fix 3.1 (F-17), walkthrough-fixes 3.9 D3: the rows, not only the box that scrolls on a phone.
        var share = "() => { const t = document.querySelector('.queue-table'); const frame = t.parentElement;"
                + " const style = getComputedStyle(frame);"
                + " return t.querySelector('tr').getBoundingClientRect().width / (frame.clientWidth"
                + " - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight)); }";

        for (var measured : onTheModeratorsTables(share)) {
            assertThat(measured.value()).as(measured.where()).isGreaterThan(0.95);
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_wide_screen_each_cell_of_a_moderators_table_sits_on_its_rows_middle_line() {
        // Walkthrough-fixes 3.9, D3 and D4: "Review", and the tags, sat apart from the rest of the row.
        var spread = "() => { const middles = [...document.querySelector('.queue-table tbody tr').children]"
                + ".map(td => { const r = (td.firstElementChild || td).getBoundingClientRect();"
                + " return r.top + r.height / 2; });"
                + " return Math.max(...middles) - Math.min(...middles); }";

        for (var measured : onTheModeratorsTables(spread)) {
            assertThat(measured.value()).as(measured.where()).isLessThan(3);
        }
    }

    private record Measured(String where, double value) {}

    private List<Measured> onTheModeratorsTables(String script) {
        var measured = new ArrayList<Measured>();
        for (var width : List.of(1280, 1920)) {
            try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(width, 900))) {
                var page = context.newPage();
                signIn(page);
                for (var path : List.of("/moderate/queue", "/moderate/articles")) {
                    page.navigate("http://localhost:" + port + path);
                    measured.add(new Measured(
                            path + " at " + width + " px", ((Number) page.evaluate(script)).doubleValue()));
                }
            }
        }
        return measured;
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_wide_screen_the_article_has_its_sidebar_beside_the_text_as_its_design_screen_draws() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/articles/registering-with-frro");
            var head = page.locator(".article-head").boundingBox();
            var text = page.locator(".article-main").boundingBox();
            var side = page.locator(".article-side").boundingBox();

            assertThat(side.x).as("the sidebar starts right of the text").isGreaterThanOrEqualTo(text.x + text.width);
            assertThat(side.y).as("level with the title").isEqualTo(head.y);
            assertThat(page.locator(".article-side .button-primary").textContent())
                    .isEqualTo("Propose an edit");
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-008
    void on_a_phone_the_articles_actions_come_under_its_title_before_the_text() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/articles/registering-with-frro");
            var head = page.locator(".article-head").boundingBox();
            var actions = page.locator(".article-actions").boundingBox();
            var text = page.locator(".article-main").boundingBox();

            assertThat(actions.y).as("under the title").isGreaterThanOrEqualTo(head.y + head.height);
            assertThat(text.y).as("before the text").isGreaterThanOrEqualTo(actions.y + actions.height);
        }
    }

    @org.junit.jupiter.api.Test
    // trace:FR-009
    void on_a_wide_screen_the_landing_page_has_a_full_width_hero_and_the_tags_beside_the_cards() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/");
            var hero = page.locator(".hero").boundingBox();
            var cards = page.locator(".landing-main").boundingBox();
            var tags = page.locator(".landing-side").boundingBox();
            var window = ((Number) page.evaluate("document.documentElement.clientWidth")).doubleValue();

            assertThat(hero.x).as("the hero starts at the window's edge").isZero();
            assertThat(hero.width).as("and spans it").isEqualTo(window);
            assertThat(tags.x).as("the tags right of the cards").isGreaterThanOrEqualTo(cards.x + cards.width);
            assertThat(page.locator(".landing-main .card-grid")
                            .first()
                            .evaluate("e => getComputedStyle(e).gridTemplateColumns.split(' ').length"))
                    .as("two cards across")
                    .isEqualTo(2);
        }
    }

    @org.junit.jupiter.api.Test
    // trace:FR-009
    void on_a_phone_the_landing_pages_tags_follow_the_cards() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/");
            var cards = page.locator(".landing-main").boundingBox();
            var tags = page.locator(".landing-side").boundingBox();

            assertThat(tags.y).isGreaterThanOrEqualTo(cards.y + cards.height);
        }
    }

    @org.junit.jupiter.api.Test
    // trace:NFR-007
    void the_focus_ring_is_the_sites_maroon_not_the_browsers_blue() {
        try (var context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900))) {
            var page = context.newPage();
            page.navigate("http://localhost:" + port + "/submit");
            page.locator("button[type=submit]").focus();
            page.keyboard().press("Shift+Tab");
            page.keyboard().press("Tab");

            var ring = page.evaluate("() => { const s = getComputedStyle(document.activeElement);"
                    + " return s.outlineStyle + ' ' + s.outlineWidth + ' ' + s.outlineColor; }");

            assertThat(ring).isEqualTo("solid 3px rgb(120, 31, 25)");
        }
    }

    private void signIn(Page page) {
        page.navigate("http://localhost:" + port + "/moderate/login");
        page.fill("input[name=password]", PASSWORD);
        page.click("button[type=submit]");
        page.waitForURL(url -> !url.endsWith("/moderate/login"));
    }

    /** The built GET routes of routes.yml with their variables filled in, and the not-found page. */
    @SuppressWarnings("unchecked")
    private static List<String> pages() throws IOException {
        Map<String, Object> contract = new Yaml().load(Files.readString(RouteContractTest.CONTRACT));
        var samples =
                Map.of("{title}", "registering-with-frro", "{number}", NUMBER, "{query}", "frro", "{tag}", "visa");
        var pages = new ArrayList<String>();
        for (var route : (List<Map<String, Object>>) contract.get("routes")) {
            if (!"built".equals(route.get("status")) || !((List<String>) route.get("methods")).contains("GET")) {
                continue;
            }
            // GET /media/{id} answers bytes, not a page; the media it serves are measured on the article.
            if ("media".equals(route.get("slice"))) {
                continue;
            }
            var path = (String) route.get("path");
            if (route.get("query") != null) {
                path += "?" + route.get("query");
            }
            for (var sample : samples.entrySet()) {
                path = path.replace(sample.getKey(), sample.getValue());
            }
            assertThat(path)
                    .as("a sample value for every variable of %s", route.get("path"))
                    .doesNotContain("{");
            pages.add(path);
        }
        pages.add("/articles/no-such-article");
        pages.add("/articles/" + ArticleAddress.slugOf(LONG_TITLE).orElseThrow());
        return pages;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        return (List<String>) value;
    }

    /**
     * Whether an element is inside a box that hides its overflow and has no width or no height, so none
     * of it shows. The editor's CodeMirror takes keystrokes through such a textarea, 1000 by 13 px
     * inside a 3-by-0 box (FR-027); it is neither text anyone reads nor a target anyone taps. So is an
     * element clipped to nothing or fully transparent on its own, as Tom Select and FilePond hide the
     * controls they replace (ADR-0022).
     */
    private static final String CLIPPED_AWAY =
            """
            e => {
              // Hidden on its own, as a library hides the control it replaces (Tom Select's select,
              // FilePond's file input, ADR-0022): nothing of it is drawn, so it is neither text nor a
              // target; FilePond's whole drop zone is what is tapped.
              const own = getComputedStyle(e);
              if (own.clipPath === 'inset(50%)' || own.clip === 'rect(0px, 0px, 0px, 0px)') return true;
              if (own.opacity === '0') return true;
              for (let box = e.parentElement; box; box = box.parentElement) {
                const r = box.getBoundingClientRect();
                if (getComputedStyle(box).overflow === 'hidden' && (r.width === 0 || r.height === 0)) return true;
              }
              return false;
            }""";

    /**
     * Visible text under 16 CSS pixels, from the element that holds it. Chips and the pinned badge
     * are labels rather than body text (docs/design/reference.md) and, with what is clipped away,
     * the only exemptions.
     */
    private static final String SMALL_TEXT =
            """
            () => [...document.querySelectorAll('body *')]
              .filter(e => e.offsetParent !== null)
              .filter(e => !(CLIPPED_AWAY)(e))
              .filter(e => [...e.childNodes].some(n => n.nodeType === Node.TEXT_NODE && n.textContent.trim() !== '')
                        || e.matches('input:not([type=hidden]), textarea'))
              .filter(e => !e.closest('.chip, .pinned-badge'))
              .filter(e => parseFloat(getComputedStyle(e).fontSize) < 16)
              .map(e => 'text under 16 px: <' + e.tagName.toLowerCase() + ' class="' + e.className + '"> "'
                        + (e.textContent || e.name || '').trim().slice(0, 30) + '"')
            """
                    .replace("CLIPPED_AWAY", CLIPPED_AWAY);

    /**
     * Buttons, controls and navigation links smaller than 44 by 44. A link is exempt only inside
     * running text: its paragraph holds words besides the link's own.
     */
    private static final String SMALL_TARGETS =
            """
            () => [...document.querySelectorAll('a, button, input:not([type=hidden]), textarea, select')]
              .filter(e => e.offsetParent !== null)
              .filter(e => !(CLIPPED_AWAY)(e))
              .filter(e => {
                if (e.tagName !== 'A') return true;
                const block = e.closest('p, li, .article-body');
                return !block || block.textContent.trim() === e.textContent.trim();
              })
              .map(e => [e, e.getBoundingClientRect()])
              .filter(([e, r]) => r.width < 44 || r.height < 44)
              .map(([e, r]) => 'target under 44 px: <' + e.tagName.toLowerCase() + '> "'
                        + (e.textContent || e.name || '').trim().slice(0, 30) + '" is '
                        + Math.round(r.width) + ' x ' + Math.round(r.height))
            """
                    .replace("CLIPPED_AWAY", CLIPPED_AWAY);

    /** The innermost elements that reach past the right edge of the window: what to fix. */
    private static final String PAST_THE_EDGE =
            """
            () => [...document.querySelectorAll('body *')]
              .filter(e => e.getBoundingClientRect().right > window.innerWidth + 1)
              .filter(e => ![...e.children].some(c => c.getBoundingClientRect().right > window.innerWidth + 1))
              .slice(0, 3)
              .map(e => '<' + e.tagName.toLowerCase() + ' class="' + e.className + '">')
              .join(', ')
            """;

    /** Whether the typeface loaded: a page whose font CSS 404s falls back silently to system-ui. */
    private static final String FONT_LOADED =
            """
            async () => {
              await document.fonts.load('16px "Noto Sans"');
              return [...document.fonts].some(f => f.family.replace(/"/g, '') === 'Noto Sans' && f.status === 'loaded');
            }
            """;
}
