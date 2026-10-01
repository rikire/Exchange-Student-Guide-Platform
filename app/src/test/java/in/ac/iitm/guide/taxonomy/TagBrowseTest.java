package in.ac.iitm.guide.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FEAT-008 through the real page, with committed rows: the test is not transactional, so a tag list
 * the page forgot to fetch fails here instead of hiding.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TagBrowseTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-28T12:00:00+05:30");

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
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-008
    void a_published_article_carrying_the_tag_appears() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        assertThat(browse("/tags/visa")).contains("href=\"/articles/registering-with-frro\"");
    }

    @Test
    // trace:FR-008
    void a_published_article_not_carrying_the_tag_does_not_appear() throws Exception {
        publish("Registering with FRRO", NOW, "visa");
        publish("Hostel mess timings", NOW, "hostel");

        assertThat(browse("/tags/visa")).doesNotContain("/articles/hostel-mess-timings");
    }

    @Test
    // trace:FR-008
    void a_tag_only_a_submission_not_yet_approved_carries_shows_no_result() throws Exception {
        publish("Registering with FRRO", NOW, "visa");
        submit("Snorkelling in Kovalam", SubmissionStatus.PENDING, "beaches");

        mockMvc.perform(get("/tags/beaches")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-008
    void a_tag_only_a_rejected_submission_carries_shows_no_result() throws Exception {
        publish("Registering with FRRO", NOW, "visa");
        submit("Paragliding in Yelagiri", SubmissionStatus.REJECTED, "adventure");

        mockMvc.perform(get("/tags/adventure")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-008
    void a_submission_carrying_a_tag_that_articles_also_carry_is_not_listed() throws Exception {
        publish("Registering with FRRO", NOW, "visa");
        submit("Visa extension myths", SubmissionStatus.PENDING, "visa");

        assertThat(browse("/tags/visa")).doesNotContain("Visa extension myths");
    }

    @Test
    // trace:FR-008
    void a_removed_article_does_not_appear() throws Exception {
        publish("Registering with FRRO", NOW, "visa");
        var removed = anArticle("Old visa rules", NOW);
        removed.setRemovedAt(NOW);
        save(removed, "visa");

        assertThat(browse("/tags/visa")).doesNotContain("/articles/old-visa-rules");
    }

    @Test
    // trace:FR-008
    void a_tag_only_removed_articles_carry_answers_404() throws Exception {
        var removed = anArticle("Old visa rules", NOW);
        removed.setRemovedAt(NOW);
        save(removed, "retired");

        mockMvc.perform(get("/tags/retired")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-008
    void the_address_ignores_letter_case() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        assertThat(browse("/tags/VISA")).contains("href=\"/articles/registering-with-frro\"");
    }

    @Test
    // trace:FR-008
    void tags_that_differ_only_in_punctuation_share_one_page() throws Exception {
        publish("Registering with FRRO", NOW, "visa/frro");
        publish("Your first week", NOW, "visa frro");

        assertThat(browse("/tags/visa-frro"))
                .contains("href=\"/articles/registering-with-frro\"")
                .contains("href=\"/articles/your-first-week\"");
    }

    @Test
    // trace:FR-008
    void an_address_no_tag_has_answers_404() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        mockMvc.perform(get("/tags/skydiving")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-008
    void the_most_recently_updated_article_comes_first() throws Exception {
        publish("Older guide", NOW.minusDays(9), "visa");
        publish("Newer guide", NOW.minusDays(1), "visa");

        var page = browse("/tags/visa");

        assertThat(page.indexOf("/articles/newer-guide"))
                .isPositive()
                .isLessThan(page.indexOf("/articles/older-guide"));
    }

    @Test
    // trace:FR-008
    void the_page_names_the_tag_and_shows_the_summary_and_the_date() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        assertThat(browse("/tags/visa"))
                .contains(">visa<")
                .contains("A summary of Registering with FRRO.")
                .contains("28 Sep 2026");
    }

    @Test
    // trace:FR-008
    void the_first_fifty_are_listed_with_the_total() throws Exception {
        for (int i = 0; i < 51; i++) {
            publish(String.format("Guide number %03d", i), NOW.minusDays(i), "visa");
        }

        var page = browse("/tags/visa");

        assertThat(page)
                .contains("51 articles")
                .contains("/articles/guide-number-049")
                .doesNotContain("/articles/guide-number-050");
    }

    @Test
    // trace:FR-008
    void the_date_on_a_card_is_the_day_in_india() throws Exception {
        publish("Registering with FRRO", OffsetDateTime.parse("2026-09-30T20:00:00Z"), "visa");

        var page = mockMvc.perform(get("/tags/visa")).andReturn().getResponse().getContentAsString();

        assertThat(page).contains(">1 Oct 2026</relative-time>");
    }

    @Test
    // trace:FR-008
    void the_date_is_in_english_whatever_language_the_browser_asks_for() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        var page = mockMvc.perform(get("/tags/visa").header("Accept-Language", "ru-RU"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page).contains(">28 Sep 2026</relative-time>");
    }

    @Test
    // trace:FR-008
    void a_tag_on_the_landing_page_links_to_its_page() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        assertThat(browse("/")).contains("href=\"/tags/visa\"");
    }

    @Test
    // trace:FR-008
    void a_tag_on_an_article_links_to_its_page() throws Exception {
        publish("Registering with FRRO", NOW, "sim card");

        assertThat(browse("/articles/registering-with-frro")).contains("href=\"/tags/sim-card\"");
    }

    @Test
    // trace:FR-008
    void a_tag_on_a_search_result_links_to_its_page() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        assertThat(mockMvc.perform(get("/search").param("q", "frro"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .contains("href=\"/tags/visa\"");
    }

    @Test
    // trace:FR-008
    void a_tag_with_no_address_is_shown_but_not_linked() throws Exception {
        publish("Registering with FRRO", NOW, "!!!");

        assertThat(browse("/articles/registering-with-frro")).contains(">!!!<").doesNotContain("href=\"/tags/");
    }

    @Test
    // trace:FR-031
    void opening_a_tags_page_counts_a_visit_each_time() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        browse("/tags/visa");
        browse("/tags/VISA");

        assertThat(visitsOf("visa")).isEqualTo(2);
    }

    @Test
    // trace:FR-031
    void a_tag_page_that_answers_404_counts_no_visit() throws Exception {
        submit("Kodaikanal trip", SubmissionStatus.PENDING, "trips");

        mockMvc.perform(get("/tags/trips")).andExpect(status().isNotFound());

        assertThat(visitsOf("trips")).isZero();
    }

    @Test
    // trace:FR-031
    void every_tag_behind_one_address_counts_the_visit() throws Exception {
        publish("Registering with FRRO", NOW, "visa/frro");
        publish("FRRO office hours", NOW, "visa frro");

        browse("/tags/visa-frro");

        assertThat(visitsOf("visa/frro")).isEqualTo(1);
        assertThat(visitsOf("visa frro")).isEqualTo(1);
    }

    private long visitsOf(String tag) {
        return jdbc.queryForObject("SELECT visit_count FROM tag WHERE name = ?", Long.class, tag);
    }

    private String browse(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    // trace:FR-008
    void the_tag_index_lists_every_tag_a_published_article_carries_by_name_with_its_count() throws Exception {
        publish("Hostel rules", NOW, "visa", "hostel");
        publish("Visa extension", NOW, "visa");
        var removed = anArticle("Old shuttle", NOW);
        removed.setRemovedAt(NOW);
        save(removed, "shuttle");
        submit("Snorkelling in Kovalam", SubmissionStatus.PENDING, "snorkelling");

        var page = mockMvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page.indexOf("href=\"/tags/hostel\""))
                .as("hostel before visa")
                .isPositive()
                .isLessThan(page.indexOf("href=\"/tags/visa\""));
        assertThat(page)
                .containsPattern(
                        "href=\"/tags/visa\"[^>]*>\\s*<span[^>]*>visa</span>\\s*<span class=\"chip-count\">2</span>")
                .doesNotContain("/tags/shuttle")
                .doesNotContain("/tags/snorkelling");
    }

    @Test
    // trace:FR-008
    void the_tag_index_shows_the_first_five_hundred_tags_and_says_there_are_more() throws Exception {
        publish("Everything", NOW);
        var article = jdbc.queryForObject("SELECT id FROM article", java.util.UUID.class);
        for (var i = 0; i < 501; i++) {
            var tag = java.util.UUID.randomUUID();
            jdbc.update("INSERT INTO tag (id, name) VALUES (?, ?)", tag, "tag%03d".formatted(i));
            jdbc.update("INSERT INTO article_tag (article_id, tag_id) VALUES (?, ?)", article, tag);
        }

        var page = mockMvc.perform(get("/tags")).andReturn().getResponse().getContentAsString();

        assertThat(page).contains("href=\"/tags/tag499\"").doesNotContain("href=\"/tags/tag500\"");
        assertThat(page).contains("Showing the first 500 tags");
    }

    @Test
    // trace:FR-008
    void with_no_published_article_the_tag_index_says_there_are_no_tags_yet() throws Exception {
        assertThat(mockMvc.perform(get("/tags")).andReturn().getResponse().getContentAsString())
                .contains("No tags yet");
    }

    private void publish(String title, OffsetDateTime updatedAt, String... tags) {
        save(anArticle(title, updatedAt), tags);
    }

    private Article anArticle(String title, OffsetDateTime updatedAt) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary of " + title + ".");
        article.setBody(title + " in a few words.");
        article.setPublishedAt(updatedAt);
        article.setUpdatedAt(updatedAt);
        return article;
    }

    private void save(Article article, String... tagNames) {
        transaction.executeWithoutResult(status -> {
            article.setTags(tags(tagNames));
            entityManager.persist(article);
        });
    }

    private void submit(String title, SubmissionStatus status, String... tagNames) {
        transaction.executeWithoutResult(tx -> {
            var submission = new Submission();
            submission.setSubmissionNumber(String.format("S-%06d", Math.abs(title.hashCode() % 1_000_000)));
            submission.setType(SubmissionType.NEW_ARTICLE);
            submission.setTitle(title);
            submission.setSummary("A summary of " + title + ".");
            submission.setBody(title + " is a day trip from campus.");
            submission.setStatus(status);
            submission.setSubmittedAt(NOW);
            if (status != SubmissionStatus.PENDING) {
                submission.setDecidedAt(NOW);
            }
            submission.setTags(tags(tagNames));
            entityManager.persist(submission);
        });
    }

    /** One row per name, as ADR-0005 has it: a second article with the tag reuses the first's row. */
    private Set<Tag> tags(String... names) {
        var tags = new HashSet<Tag>();
        for (var name : names) {
            var existing = entityManager
                    .createQuery("select t from Tag t where t.name = :name", Tag.class)
                    .setParameter("name", name)
                    .getResultList();
            if (existing.isEmpty()) {
                var tag = new Tag();
                tag.setName(name);
                entityManager.persist(tag);
                tags.add(tag);
            } else {
                tags.add(existing.get(0));
            }
        }
        return tags;
    }
}
