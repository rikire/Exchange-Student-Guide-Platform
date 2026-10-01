package in.ac.iitm.guide.articleview;

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

/** FR-033: the list of every published article, its pages, its orders and its tag. */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleListTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-01T12:00:00Z");

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
    // trace:FR-033
    void fifty_articles_by_title_fill_the_first_page_and_the_rest_go_to_the_next() throws Exception {
        for (var i = 1; i <= 51; i++) {
            publish("Article %02d".formatted(i), NOW);
        }

        var first = page("/articles");
        var second = page("/articles?page=2");

        assertThat(first).contains("Article 01", "Article 50").doesNotContain("Article 51");
        assertThat(first.indexOf("Article 01")).isLessThan(first.indexOf("Article 02"));
        assertThat(second).contains("Article 51").doesNotContain("Article 50");
    }

    @Test
    // trace:FR-033
    void neither_a_submission_nor_a_removed_article_is_listed() throws Exception {
        publish("Registering with FRRO", NOW);
        publish("Old hostel rules", NOW);
        jdbc.update("UPDATE article SET removed_at = ? WHERE slug = ?", NOW, "old-hostel-rules");
        pending("Getting a SIM card");

        var list = page("/articles");

        assertThat(list).contains("Registering with FRRO").doesNotContain("Old hostel rules", "Getting a SIM card");
    }

    @Test
    // trace:FR-033
    void ordered_by_most_viewed_the_list_follows_the_view_counts() throws Exception {
        publish("Five", NOW);
        publish("Zero", NOW);
        publish("Two", NOW);
        jdbc.update("UPDATE article SET view_count = 5 WHERE slug = 'five'");
        jdbc.update("UPDATE article SET view_count = 2 WHERE slug = 'two'");

        var list = page("/articles?sort=views");

        assertThat(list.indexOf(">Five<")).isLessThan(list.indexOf(">Two<"));
        assertThat(list.indexOf(">Two<")).isLessThan(list.indexOf(">Zero<"));
    }

    @Test
    // trace:FR-033
    void ordered_by_recently_updated_the_newest_comes_first() throws Exception {
        publish("Older", NOW.minusDays(3));
        publish("Newer", NOW);

        var list = page("/articles?sort=updated");

        assertThat(list.indexOf(">Newer<")).isLessThan(list.indexOf(">Older<"));
    }

    @Test
    // trace:FR-033
    void narrowed_to_a_tag_only_the_articles_carrying_it_are_listed() throws Exception {
        publish("Registering with FRRO", NOW, "visa");
        publish("Eating on campus", NOW, "food");

        var list = page("/articles?tag=visa");

        assertThat(list).contains("Registering with FRRO").doesNotContain("Eating on campus");
    }

    @Test
    // trace:FR-033
    void a_tag_no_published_article_carries_says_so_and_offers_the_full_list() throws Exception {
        publish("Registering with FRRO", NOW, "visa");

        var list = page("/articles?tag=no-such-tag");

        assertThat(list).contains("No published article carries this tag.").contains("href=\"/articles\"");
        assertThat(list).doesNotContain("Registering with FRRO");
    }

    @Test
    // trace:FR-033
    void an_order_the_list_does_not_offer_or_a_page_below_one_answers_400() throws Exception {
        mockMvc.perform(get("/articles?sort=random")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/articles?page=0")).andExpect(status().isBadRequest());
    }

    @Test
    // trace:FR-033
    void a_page_past_the_last_answers_404() throws Exception {
        publish("Registering with FRRO", NOW);

        mockMvc.perform(get("/articles?page=2")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-033
    void the_landing_page_links_to_the_list() throws Exception {
        assertThat(page("/")).contains("href=\"/articles\"");
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void publish(String title, OffsetDateTime updatedAt, String... tags) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("About " + title + ".");
        article.setBody("Text.");
        article.setPublishedAt(updatedAt);
        article.setUpdatedAt(updatedAt);
        var tagRows = new HashSet<Tag>();
        transaction.executeWithoutResult(status -> {
            for (var name : tags) {
                var existing = entityManager
                        .createQuery("select t from Tag t where t.name = :name", Tag.class)
                        .setParameter("name", name)
                        .getResultStream()
                        .findFirst();
                tagRows.add(existing.orElseGet(() -> {
                    var tag = new Tag();
                    tag.setName(name);
                    entityManager.persist(tag);
                    return tag;
                }));
            }
            article.setTags(tagRows.isEmpty() ? Set.of() : tagRows);
            entityManager.persist(article);
        });
    }

    private void pending(String title) {
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-LIST-0000-0001");
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle(title);
        submission.setSummary("A summary.");
        submission.setBody("Text.");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(NOW);
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(s -> entityManager.persist(submission));
    }
}
