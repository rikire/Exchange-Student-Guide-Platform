package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-031's article count beside a tag reads at a glance: a badge of its own inside the chip, in the
 * text's own colour and weight, rather than pale grey on the chip's ground. The human did not see the
 * counts on 10 Oct. Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserTagIndexTest {

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

    @BeforeAll
    void startTheBrowserAndPublishATaggedArticle() {
        playwright = Playwright.create();
        browser = TestBrowser.launch(playwright);
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle("Registering with FRRO");
            article.setSlug(ArticleAddress.slugOf(article.getTitle()).orElseThrow());
            article.setSummary("Register within 14 days.");
            article.setBody("Bring your passport.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(new LinkedHashSet<>(tags.named(List.of("visa"))));
            entityManager.persist(article);
        });
    }

    @AfterAll
    void closeTheBrowserAndClearTheDatabase() {
        browser.close();
        playwright.close();
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-031
    void the_count_beside_a_tag_is_a_badge_in_the_texts_colour_and_weight() {
        var page = browser.newPage();
        page.navigate("http://localhost:" + port + "/tags");

        @SuppressWarnings("unchecked")
        var style = (Map<String, String>)
                page.evaluate("() => { const c = getComputedStyle(document.querySelector('.chip-count'));"
                        + " const ink = getComputedStyle(document.body).color;"
                        + " return {color: c.color, ink: ink, weight: c.fontWeight, ground: c.backgroundColor}; }");

        assertThat(style.get("color")).as("the count's colour").isEqualTo(style.get("ink"));
        assertThat(Integer.parseInt(style.get("weight")))
                .as("the count's weight")
                .isGreaterThanOrEqualTo(700);
        assertThat(style.get("ground")).as("a ground of its own").isNotEqualTo("rgba(0, 0, 0, 0)");
    }
}
