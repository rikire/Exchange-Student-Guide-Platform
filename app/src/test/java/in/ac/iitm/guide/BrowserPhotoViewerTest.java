package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaTestFiles;
import in.ac.iitm.guide.media.Upload;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-032's photo viewer in a real browser, under the strict Content-Security-Policy (ADR-0013,
 * ADR-0018): every photo on an article page, in its text or attached, opens full screen, zooms, and
 * pages on to the others; the keyboard reaches it; the moderator's review page has it too. Every
 * test also fails on a policy violation or a script error. Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserPhotoViewerTest {

    private static final String PASSWORD = "the office's password";
    private static final String NUMBER = "SUB-PHOT-0000-0001";
    private static final String CURRENT_IMAGE =
            ".pswp__item:not([aria-hidden=true]) .pswp__img:not(.pswp__img--placeholder)";

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
    private BrowserContext context;
    private final List<String> problems = new ArrayList<>();

    /**
     * Two photos in the text, served from the site's own images, and one attached photo moved to the
     * article as approval moves it; and a pending edit with a photo, for the review page.
     */
    @BeforeAll
    void startTheBrowserAndPublishAnArticleWithPhotos() {
        playwright = Playwright.create();
        browser = TestBrowser.launch(playwright);
        var now = OffsetDateTime.now();
        var article = new Article();
        transaction.executeWithoutResult(status -> {
            article.setTitle("Beaches");
            article.setSlug(ArticleAddress.slugOf(article.getTitle()).orElseThrow());
            article.setSummary("The sea at evening.");
            article.setBody("Evening.\n\n![Sunset at Besant Nagar](/img/articles/beaches/besant-nagar-sunset.jpg)\n\n"
                    + "Morning.\n\n![Waves on the Marina](/img/articles/beaches/marina-beach.jpg)\n");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(Set.of());
            entityManager.persist(article);
        });
        var published = submission("SUB-PHOT-0000-0000", SubmissionStatus.APPROVED, article, now);
        media.attach(published.getId(), photo("stall.jpg", 900, 600));
        media.moveToArticle(published.getId(), article.getId());

        var pending = submission(NUMBER, SubmissionStatus.PENDING, article, now);
        media.attach(pending.getId(), photo("form.jpg", 600, 800));
    }

    @AfterAll
    void closeTheBrowserAndClearTheDatabase() throws IOException {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        MediaTestFiles.empty(MediaTestFiles.ROOT);
    }

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
    // trace:FR-032
    void a_photo_in_the_text_opens_full_screen_when_clicked() {
        var page = open("/articles/beaches");

        page.click(".article-body img >> nth=0");

        assertThat(currentPhoto(page)).endsWith("/img/articles/beaches/besant-nagar-sunset.jpg");
    }

    @Test
    // trace:FR-032
    void the_viewer_pages_through_every_photo_on_the_page_attached_ones_included() {
        var page = open("/articles/beaches");
        page.click(".article-body img >> nth=0");
        currentPhoto(page);

        page.keyboard().press("ArrowRight");
        assertThat(currentPhoto(page)).endsWith("/img/articles/beaches/marina-beach.jpg");
        page.keyboard().press("ArrowRight");

        assertThat(currentPhoto(page)).contains("/media/");
        assertThat(page.textContent(".pswp__counter")).isEqualTo("3 / 3");
    }

    @Test
    // trace:FR-032
    void the_zoom_control_enlarges_the_photo_past_the_screens_fit() {
        var page = open("/articles/beaches");
        page.click(".article-body img >> nth=0");
        currentPhoto(page);
        var fitted = page.locator(CURRENT_IMAGE).boundingBox().width;

        page.click(".pswp__button--zoom");

        // The zoom animates; wait for where it ends rather than for a fixed time.
        page.waitForFunction(
                "([sel, fitted]) => document.querySelector(sel).getBoundingClientRect().width > fitted * 1.4",
                List.of(CURRENT_IMAGE, fitted));
    }

    @Test
    // trace:FR-032
    void a_photo_opens_from_the_keyboard_and_escape_closes_it_and_returns_the_focus() {
        var page = open("/articles/beaches");
        page.focus(".article-body img >> nth=1");

        page.keyboard().press("Enter");
        assertThat(currentPhoto(page)).endsWith("/img/articles/beaches/marina-beach.jpg");
        page.keyboard().press("Escape");
        page.waitForSelector(
                ".pswp",
                new Page.WaitForSelectorOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));

        assertThat(page.evaluate("() => document.activeElement.getAttribute('src')"))
                .isEqualTo("/img/articles/beaches/marina-beach.jpg");
    }

    @Test
    // trace:FR-032
    void a_photo_offers_itself_to_a_screen_reader_as_opening_the_viewer() {
        var page = open("/articles/beaches");

        var photo = page.locator(".article-body img >> nth=0");

        assertThat(photo.getAttribute("role")).isEqualTo("button");
        assertThat(photo.getAttribute("tabindex")).isEqualTo("0");
        assertThat(photo.getAttribute("aria-label")).isEqualTo("Open photo: Sunset at Besant Nagar");
    }

    @Test
    // trace:FR-032
    void a_photo_on_the_moderators_review_page_opens_in_the_viewer() {
        var page = context.newPage();
        watch(page);
        page.navigate("http://localhost:" + port + "/moderate/login");
        page.fill("input[name=password]", PASSWORD);
        page.click("main button[type=submit]");
        page.waitForURL(url -> !url.endsWith("/moderate/login"));
        page.navigate("http://localhost:" + port + "/moderate/submissions/" + NUMBER);

        page.click(".media-photo img");

        assertThat(currentPhoto(page)).contains("/media/");
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

    /** The src of the photo the open viewer shows, once it has loaded. */
    private static String currentPhoto(Page page) {
        page.waitForSelector(".pswp--open");
        page.waitForFunction(
                "sel => { const img = document.querySelector(sel); return img && img.complete"
                        + " && img.naturalWidth > 0; }",
                CURRENT_IMAGE);
        return page.locator(CURRENT_IMAGE).getAttribute("src");
    }

    private Submission submission(String number, SubmissionStatus status, Article article, OffsetDateTime at) {
        var submission = new Submission();
        submission.setSubmissionNumber(number);
        submission.setType(SubmissionType.EDIT);
        submission.setTargetArticleId(article.getId());
        submission.setTitle(article.getTitle());
        submission.setSummary(article.getSummary());
        submission.setBody(article.getBody() + "\nWith a photo.");
        submission.setStatus(status);
        submission.setSubmittedAt(at);
        if (status != SubmissionStatus.PENDING) {
            submission.setDecidedAt(at);
        }
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(tx -> entityManager.persist(submission));
        return submission;
    }

    private static Upload photo(String name, int width, int height) {
        var bytes = MediaTestFiles.jpeg(width, height);
        return new Upload(name, bytes.length, new ByteArrayResource(bytes));
    }
}
