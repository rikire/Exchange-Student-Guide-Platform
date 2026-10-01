package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Walkthrough fix 3.3 (F-21, F-8): the landing, tag and all-articles pages list an article on one
 * card, as docs/design/screens/Landing.html draws it — its tags, the title, the summary, then when it
 * was updated. Not transactional, so a tag list a page forgot to fetch fails here.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleCardTest {

    private static final Pattern CARD =
            Pattern.compile("<article class=\"article-card\">(.*?)</article>", Pattern.DOTALL);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void publishAnArticleTaggedVisa() {
        transaction.executeWithoutResult(status -> {
            var tag = new Tag();
            tag.setName("visa");
            entityManager.persist(tag);
            var article = new Article();
            article.setTitle("Registering with FRRO");
            article.setSlug(ArticleAddress.slugOf(article.getTitle()).orElseThrow());
            article.setSummary("Register within 14 days of arriving.");
            article.setBody("Text.");
            article.setPublishedAt(OffsetDateTime.parse("2026-09-20T10:00:00+05:30"));
            article.setUpdatedAt(OffsetDateTime.parse("2026-09-21T10:00:00+05:30"));
            article.setTags(Set.of(tag));
            entityManager.persist(article);
        });
    }

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @ParameterizedTest(name = "on {0}")
    @ValueSource(strings = {"/", "/tags/visa", "/articles"})
    // trace:FR-009
    // trace:FR-008
    // trace:FR-033
    void an_article_is_listed_with_its_tags_title_summary_and_date_in_that_order(String path) throws Exception {
        var card = firstCard(path);

        assertThat(card).contains(">visa<", "Registering with FRRO", "Register within 14 days", "21 Sep 2026");
        assertThat(card.indexOf(">visa<"))
                .as("the tags above the title")
                .isLessThan(card.indexOf("article-card-title"));
        assertThat(card.indexOf("article-card-summary")).isLessThan(card.indexOf("article-card-meta"));
        assertThat(card).contains("datetime=\"2026-09-21T");
    }

    @Test
    // trace:FR-009
    void a_pinned_card_on_the_landing_page_is_the_same_card_with_its_badge_first() throws Exception {
        jdbc.update("UPDATE article SET pinned_at = CURRENT_TIMESTAMP, pin_position = 1");

        var card = firstCard("/");

        assertThat(card.indexOf("pinned-badge")).isGreaterThanOrEqualTo(0).isLessThan(card.indexOf(">visa<"));
        assertThat(card).contains("article-card-meta");
    }

    private String firstCard(String path) throws Exception {
        var page = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var card = CARD.matcher(page);
        assertThat(card.find()).as("a card on %s", path).isTrue();
        return card.group(1);
    }
}
