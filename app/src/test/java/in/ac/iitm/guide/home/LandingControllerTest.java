package in.ac.iitm.guide.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Same shape as {@code ArticleControllerTest}: committed rows, the real page. The test is not
 * transactional, so a lazy tag list the page forgot to fetch fails here instead of hiding.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LandingControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-25T12:00:00+05:30");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-009
    void pinned_articles_are_shown_before_the_recently_added_ones() throws Exception {
        publish("Older Recent", NOW.minusDays(5), null);
        publish("Pinned Guide", NOW.minusDays(40), NOW.minusDays(1));
        publish("Newer Recent", NOW.minusDays(1), null);

        var page = landing();

        assertThat(page).contains("Pinned Guide", "Older Recent", "Newer Recent");
        assertThat(page.indexOf("Pinned Guide")).isLessThan(page.indexOf("Newer Recent"));
        assertThat(page.indexOf("Pinned Guide")).isLessThan(page.indexOf("Older Recent"));
    }

    @Test
    // trace:FR-009
    void the_most_recently_added_come_first_among_the_recent() throws Exception {
        publish("Older Recent", NOW.minusDays(5), null);
        publish("Newer Recent", NOW.minusDays(1), null);

        var page = landing();

        assertThat(page.indexOf("Newer Recent")).isLessThan(page.indexOf("Older Recent"));
    }

    @Test
    // trace:FR-009
    void without_a_pinned_article_the_recent_ones_are_shown_and_no_pinned_section() throws Exception {
        publish("Only Recent", NOW.minusDays(1), null);

        var page = landing();

        assertThat(page).contains("Only Recent").doesNotContain("Pinned");
    }

    @Test
    // trace:FR-009
    void a_pinned_article_is_not_repeated_among_the_recent_ones() throws Exception {
        publish("Pinned Guide", NOW.minusDays(1), NOW.minusHours(1));

        var page = landing();

        assertThat(count(page, "href=\"/articles/pinned-guide\"")).isEqualTo(1);
    }

    @Test
    // trace:FR-009
    void among_the_pinned_the_most_recently_pinned_comes_first() throws Exception {
        publish("Pinned Earlier", NOW.minusDays(40), NOW.minusDays(10));
        publish("Pinned Lately", NOW.minusDays(50), NOW.minusDays(1));

        var page = landing();

        assertThat(page.indexOf("Pinned Lately")).isLessThan(page.indexOf("Pinned Earlier"));
    }

    @Test
    // trace:FR-009
    void when_a_list_is_cut_it_is_the_newest_that_stay() throws Exception {
        // Oldest first, so the order the database happens to return rows in is the wrong order.
        for (int i = 14; i >= 0; i--) {
            publish("Recent Number " + (100 + i), NOW.minusDays(i), null);
            publish("Pinned Number " + (100 + i), NOW.minusDays(50 + i), NOW.minusDays(i));
        }

        var page = landing();

        assertThat(page)
                .contains("href=\"/articles/recent-number-111\"")
                .doesNotContain("href=\"/articles/recent-number-112\"")
                .contains("href=\"/articles/pinned-number-111\"")
                .doesNotContain("href=\"/articles/pinned-number-112\"");
    }

    @Test
    // trace:FR-009
    void the_tag_list_is_bounded_and_alphabetical() throws Exception {
        var names = new String[55];
        for (int i = 0; i < names.length; i++) {
            names[i] = String.format("tag-%03d", i);
        }
        publish("Very Tagged", NOW.minusDays(1), null, names);

        var cloud = landing().split("class=\"tag-cloud\"", 2)[1];

        assertThat(count(cloud, "<li class=\"chip\"")).isEqualTo(50);
        assertThat(cloud).contains(">tag-049<").doesNotContain(">tag-050<");
    }

    @Test
    // trace:FR-009
    void a_search_entry_point_is_shown() throws Exception {
        var page = landing();

        assertThat(page).contains("action=\"/search\"").contains("name=\"q\"");
    }

    @Test
    // trace:FR-009
    void the_tags_in_use_are_listed() throws Exception {
        publish("Visa Guide", NOW.minusDays(1), null, "visa", "admin");

        var page = landing();

        assertThat(page).contains(">visa<").contains(">admin<");
    }

    @Test
    // trace:FR-009
    void a_tag_carried_only_by_a_removed_article_is_not_listed() throws Exception {
        publish("Live Guide", NOW.minusDays(1), null, "visa");
        var removed = anArticle("Old Guide", NOW.minusDays(9), null);
        removed.setRemovedAt(NOW);
        save(removed, "retired");

        var page = landing();

        assertThat(page).contains(">visa<").doesNotContain("retired");
    }

    @Test
    // trace:FR-009
    void an_article_card_shows_the_summary_and_leads_to_the_article() throws Exception {
        publish("Hostel Life", NOW.minusDays(1), null);

        var page = landing();

        assertThat(page).contains("A summary of Hostel Life.").contains("href=\"/articles/hostel-life\"");
    }

    @Test
    // trace:FR-009
    void a_card_for_a_title_in_devanagari_links_to_the_percent_encoded_address() throws Exception {
        publish("छात्रावास जीवन", NOW.minusDays(1), null);

        var page = landing();

        assertThat(page)
                .contains("href=\""
                        + ArticleAddress.pathOf(
                                ArticleAddress.slugOf("छात्रावास जीवन").orElseThrow()) + "\"");
    }

    @Test
    // trace:FR-009
    void a_removed_article_is_not_shown() throws Exception {
        var removed = anArticle("Old Rules", NOW.minusDays(9), null);
        removed.setRemovedAt(NOW);
        save(removed);
        publish("Live Guide", NOW.minusDays(1), null);

        var page = landing();

        assertThat(page).contains("Live Guide").doesNotContain("Old Rules");
    }

    @Test
    // trace:FR-009
    void a_guide_with_no_articles_still_opens_and_says_so() throws Exception {
        var page = landing();

        assertThat(page).contains("No articles yet").contains("action=\"/search\"");
    }

    @Test
    // trace:FR-009
    void each_section_is_bounded_so_a_large_guide_cannot_make_an_unbounded_page() throws Exception {
        for (int i = 0; i < 15; i++) {
            publish("Recent Number " + (100 + i), NOW.minusDays(i), null);
        }
        for (int i = 0; i < 15; i++) {
            publish("Pinned Number " + (100 + i), NOW.minusDays(50 + i), NOW.minusDays(i));
        }

        var page = landing();

        // One link per card: the title also appears in the card's summary, so it cannot be counted.
        assertThat(count(page, "href=\"/articles/recent-number-")).isEqualTo(12);
        assertThat(count(page, "href=\"/articles/pinned-number-")).isEqualTo(12);
    }

    @Test
    // trace:FR-009
    void html_characters_in_a_title_and_summary_are_escaped() throws Exception {
        var article = anArticle("Fees <b>& Payments", NOW.minusDays(1), null);
        article.setSummary("Pay <script>x</script> now");
        save(article);

        var page = landing();

        assertThat(page)
                .contains("Fees &lt;b&gt;&amp; Payments")
                .contains("Pay &lt;script&gt;x&lt;/script&gt; now")
                .doesNotContain("<script>x");
    }

    // ---- fixtures ----

    private String landing() throws Exception {
        return mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static int count(String text, String word) {
        return text.split(Pattern.quote(word), -1).length - 1;
    }

    private void publish(String title, OffsetDateTime publishedAt, OffsetDateTime pinnedAt, String... tags) {
        save(anArticle(title, publishedAt, pinnedAt), tags);
    }

    private Article anArticle(String title, OffsetDateTime publishedAt, OffsetDateTime pinnedAt) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary of " + title + ".");
        article.setBody("Text.");
        article.setPublishedAt(publishedAt);
        article.setUpdatedAt(publishedAt);
        article.setPinnedAt(pinnedAt);
        article.setTags(Set.of());
        return article;
    }

    private void save(Article article, String... tags) {
        var tagRows = new HashSet<Tag>();
        for (var name : tags) {
            var tag = new Tag();
            tag.setName(name);
            tagRows.add(tag);
        }
        transaction.executeWithoutResult(status -> {
            tagRows.forEach(entityManager::persist);
            article.setTags(tagRows);
            entityManager.persist(article);
        });
    }
}
