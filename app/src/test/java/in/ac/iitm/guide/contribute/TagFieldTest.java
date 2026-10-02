package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fix 3.6's tag field as the server renders it (ADR-0022): one {@code <select multiple name="tags">}
 * whose options are the tags of live articles, with the submission's own tags selected. Tom Select
 * turns it into chips in the browser; that half is BrowserTagFieldTest's.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TagFieldTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Pattern SELECT =
            Pattern.compile("<select[^>]*name=\"tags\"[^>]*>(.*?)</select>", Pattern.DOTALL);
    private static final Pattern OPTION = Pattern.compile("<option([^>]*)>([^<]*)</option>");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private Tags tags;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-010
    void the_form_offers_every_tag_of_a_live_article_once_from_a_to_z_and_selects_none() throws Exception {
        publish("Hostel Life", false, "visa", "hostel");
        publish("Mess Food", false, "food", "hostel");

        var form = page("/submit");

        assertThat(form).containsPattern("<select[^>]*name=\"tags\"[^>]*multiple");
        assertThat(options(form)).containsExactly("food", "hostel", "visa");
        assertThat(selected(form)).isEmpty();
    }

    @Test
    // trace:FR-010
    void a_tag_carried_only_by_a_removed_article_or_a_pending_submission_is_not_offered() throws Exception {
        publish("Hostel Life", false, "hostel");
        publish("Old Page", true, "gone");
        tags.named(List.of("only on a submission"));

        assertThat(options(page("/submit"))).containsExactly("hostel");
    }

    @Test
    // trace:FR-010
    void the_field_says_how_many_tags_a_submission_may_carry() throws Exception {
        assertThat(page("/submit")).containsPattern("<select[^>]*name=\"tags\"[^>]*data-most=\"10\"");
    }

    @Test
    // trace:FR-010
    void at_most_500_tags_are_offered_the_first_from_a_to_z() throws Exception {
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle("Every Tag");
            article.setSlug(ArticleAddress.slugOf("Every Tag").orElseThrow());
            article.setSummary("A summary.");
            article.setBody("Text.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            var many = new LinkedHashSet<Tag>();
            for (var i = 0; i < 501; i++) {
                var tag = new Tag();
                tag.setName("tag %03d".formatted(i));
                entityManager.persist(tag);
                many.add(tag);
            }
            article.setTags(many);
            entityManager.persist(article);
        });

        var offered = options(page("/submit"));

        assertThat(offered).hasSize(500).startsWith("tag 000").endsWith("tag 499");
    }

    @Test
    // trace:FR-011
    void the_edit_form_has_the_articles_tags_selected_among_the_others() throws Exception {
        publish("Hostel Life", false, "hostel", "visa");
        publish("Mess Food", false, "food");

        var form = page("/articles/hostel-life/edit");

        assertThat(options(form)).containsExactlyInAnyOrder("food", "hostel", "visa");
        assertThat(selected(form)).containsExactlyInAnyOrder("hostel", "visa");
    }

    @Test
    // trace:FR-010
    void a_refused_form_keeps_the_typed_tags_selected_a_new_one_and_one_differing_only_in_case_included()
            throws Exception {
        publish("Hostel Life", false, "hostel");

        var refused = submitNew("Hostel Life", "New Tag", "HOSTEL");

        assertThat(selected(refused)).as("the typed tags, as typed").containsExactly("New Tag", "HOSTEL");
        assertThat(options(refused))
                .as("a stored tag the typed one already stands for is not offered twice")
                .containsExactly("New Tag", "HOSTEL");
    }

    private void publish(String title, boolean removed, String... names) {
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody("Text.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(new LinkedHashSet<>(tags.named(List.of(names))));
            if (removed) {
                article.setRemovedAt(now);
            }
            entityManager.persist(article);
        });
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** A new article whose title is taken, so the form comes back refused with what was typed. */
    private String submitNew(String title, String... typed) throws Exception {
        var form = mockMvc.perform(get("/submit")).andExpect(status().isOk()).andReturn();
        var token = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        var request = post("/submissions")
                .session((MockHttpSession) form.getRequest().getSession())
                .param("_csrf", token.group(1))
                .param("title", title)
                .param("summary", "A summary.")
                .param("body", "Text.");
        for (var tag : typed) {
            request.param("tags", tag);
        }
        var cookies = form.getResponse().getCookies();
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        var result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        return result.getResponse().getContentAsString();
    }

    private static List<String> options(String html) {
        return matching(html, false);
    }

    private static List<String> selected(String html) {
        return matching(html, true);
    }

    private static List<String> matching(String html, boolean onlySelected) {
        var select = SELECT.matcher(html);
        assertThat(select.find()).as("the form has the tag list").isTrue();
        var found = new ArrayList<String>();
        var option = OPTION.matcher(select.group(1));
        while (option.find()) {
            if (!onlySelected || option.group(1).contains("selected")) {
                found.add(option.group(2));
            }
        }
        return found;
    }
}
