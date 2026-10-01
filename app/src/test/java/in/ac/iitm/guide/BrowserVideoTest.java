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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Walkthrough fix 1.4 in a real browser, under the strict Content-Security-Policy: a video the
 * browser can decode stays a player, and one it cannot turns into a card to download. Playwright's
 * Chromium has no H.264, so the playable one is a WebM (media/webm.webm, from
 * github.com/mathiasbynens/small, released without copyright) and an MP4 stands for one that will not
 * play; that an MP4 plays in Chrome and Safari is the human's check on the stand. Runs only under
 * {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserVideoTest {

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

    @BeforeAll
    void startTheBrowserAndPublishTwoVideos() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        publishWith("Playable", "walk.webm", MediaTestFiles.resource("media/webm.webm"));
        publishWith("Unplayable", "walk.mp4", MediaTestFiles.isoMedia("isom", 2_000));
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
    // trace:FR-001
    void a_video_the_browser_can_decode_stays_a_player() {
        var page = open("/articles/playable");

        page.waitForFunction("() => document.querySelector('video.media-video').readyState >= 1");

        assertThat(page.locator("video.media-video").isVisible()).isTrue();
        assertThat(page.locator(".media-video-card").isVisible()).isFalse();
    }

    @Test
    // trace:FR-001
    void a_video_the_browser_cannot_decode_becomes_a_card_to_download() {
        var page = open("/articles/unplayable");

        page.locator(".media-video-card").waitFor();

        assertThat(page.locator("video.media-video").isVisible()).isFalse();
        assertThat(page.locator(".media-video-card").innerText())
                .contains("This video can’t play in your browser. Download it to watch.");
    }

    private void publishWith(String title, String name, byte[] bytes) {
        var now = OffsetDateTime.now();
        var article = new Article();
        var submission = new Submission();
        transaction.executeWithoutResult(status -> {
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A walk.");
            article.setBody("A walk on campus.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(Set.of());
            entityManager.persist(article);
            submission.setSubmissionNumber("SUB-VIDE-0000-000" + title.length());
            submission.setType(SubmissionType.EDIT);
            submission.setTargetArticleId(article.getId());
            submission.setTitle(title);
            submission.setSummary("A walk.");
            submission.setBody("A walk on campus.");
            submission.setStatus(SubmissionStatus.APPROVED);
            submission.setSubmittedAt(now);
            submission.setDecidedAt(now);
            submission.setTags(new HashSet<>());
            entityManager.persist(submission);
        });
        media.attach(submission.getId(), new Upload(name, bytes.length, new ByteArrayResource(bytes)));
        media.moveToArticle(submission.getId(), article.getId());
    }

    private Page open(String path) {
        var page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                problems.add(message.text());
            }
        });
        page.onPageError(error -> problems.add(error));
        page.navigate("http://localhost:" + port + path);
        return page;
    }
}
