package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.media.MediaTestFiles;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fix 3.6's errors at the field: a refused form marks the field at fault with {@code aria-invalid}
 * and puts the message beside it, tied by {@code aria-describedby}; a refusal no field explains stays
 * in the box at the top of the form; and a form refused with a file chosen says to choose it again,
 * since no browser keeps a file across a page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FieldErrorTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Pattern INVALID =
            Pattern.compile("<(?:input|textarea|select)[^>]*name=\"(\\w+)\"[^>]*aria-invalid=\"true\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabase() throws IOException {
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
        MediaTestFiles.empty(MediaTestFiles.ROOT);
    }

    @Test
    // trace:FR-010
    void a_taken_title_is_marked_and_its_message_and_link_stand_beside_it() throws Exception {
        publish("Hostel Life");

        var page = refused(form("Hostel Life", "A summary.", "Text."));

        assertThat(invalid(page)).containsExactly("title");
        assertThat(page).containsPattern("<input[^>]*name=\"title\"[^>]*aria-describedby=\"title-error\"");
        assertThat(errorBeside(page, "title"))
                .contains("An article with this title already exists")
                .contains("href=\"/articles/hostel-life/edit\"");
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(
            delimiter = '|',
            value = {
                "''|A summary.|Text.|title|Give the article a title.",
                "'!!!'|A summary.|Text.|title|The title needs at least one letter or digit.",
                "A title|''|Text.|summary|Give the article a summary.",
                "A title|A summary.|''|body|The article has no text."
            })
    // trace:FR-010
    void each_check_marks_its_own_field(String title, String summary, String body, String field, String message)
            throws Exception {
        var page = refused(form(title, summary, body));

        assertThat(invalid(page)).containsExactly(field);
        assertThat(errorBeside(page, field)).contains(message);
    }

    @Test
    // trace:FR-010
    void a_summary_over_its_limit_marks_the_summary() throws Exception {
        var page = refused(form("A title", "s".repeat(301), "Text."));

        assertThat(invalid(page)).containsExactly("summary");
    }

    @Test
    // trace:FR-010
    void more_tags_than_allowed_mark_the_tag_list() throws Exception {
        var request = form("A title", "A summary.", "Text.");
        IntStream.rangeClosed(1, 11).forEach(i -> request.param("tags", "tag-" + i));

        var page = refused(request);

        assertThat(invalid(page)).containsExactly("tags");
        assertThat(errorBeside(page, "tags")).contains("More than 10 tags");
    }

    @Test
    // trace:FR-010
    void a_refused_file_is_marked_beside_the_file_field_and_must_be_chosen_again() throws Exception {
        var page = refused(form("A title", "A summary.", "Text.")
                .file(new MockMultipartFile("attachment", "drawing.svg", "image/svg+xml", "<svg/>".getBytes())));

        assertThat(invalid(page)).containsExactly("attachment");
        assertThat(errorBeside(page, "attachment")).contains("Choose the file again");
    }

    @Test
    // trace:FR-010
    void a_form_refused_for_another_field_with_a_file_chosen_says_to_choose_the_file_again() throws Exception {
        publish("Hostel Life");

        var page = refused(form("Hostel Life", "A summary.", "Text.")
                .file(new MockMultipartFile("attachment", "hostel.jpg", "image/jpeg", MediaTestFiles.jpeg(40, 30))));

        assertThat(invalid(page)).containsExactly("title");
        assertThat(errorBeside(page, "attachment")).contains("Choose the file again");
    }

    @Test
    // trace:FR-010
    void a_form_refused_with_no_file_chosen_does_not_ask_for_one_again() throws Exception {
        var page = refused(form("", "A summary.", "Text."));

        assertThat(page).doesNotContain("Choose the file again");
    }

    @Test
    // trace:FR-010
    void the_box_at_the_top_is_inside_the_form_so_it_is_no_wider_than_the_fields() throws Exception {
        var page = refused(form("", "A summary.", "Text."));

        assertThat(page)
                .containsPattern("(?s)<form[^>]*class=\"submission-form\".*?class=\"form-error\".*?name=\"title\"");
    }

    @Test
    // trace:FR-011
    void a_refusal_no_field_explains_stays_in_the_box_and_marks_no_field() throws Exception {
        publish("Hostel Life");
        var form = mockMvc.perform(get("/articles/hostel-life/edit")).andReturn();
        var html = form.getResponse().getContentAsString();
        var csrf = CSRF.matcher(html);
        var article = Pattern.compile("name=\"article\" value=\"([^\"]+)\"").matcher(html);
        assertThat(csrf.find() && article.find()).isTrue();
        jdbc.update("UPDATE article SET removed_at = CURRENT_TIMESTAMP");

        var result = mockMvc.perform(multipart("/articles/hostel-life/edits")
                        .session((MockHttpSession) form.getRequest().getSession())
                        .cookie(form.getResponse().getCookies())
                        .param("_csrf", csrf.group(1))
                        .param("article", article.group(1))
                        .param("title", "Hostel Life")
                        .param("summary", "Rooms.")
                        .param("body", "Text."))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        var page = result.getResponse().getContentAsString();
        assertThat(invalid(page)).isEmpty();
        assertThat(page).containsPattern("(?s)class=\"form-error\".*?removed while you were editing");
    }

    /** The new-article form, with the token and session it was served with, ready to post. */
    private MockMultipartHttpServletRequestBuilder form(String title, String summary, String body) throws Exception {
        var form = mockMvc.perform(get("/submit")).andExpect(status().isOk()).andReturn();
        var token = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        var request = multipart("/submissions");
        request.session((MockHttpSession) form.getRequest().getSession())
                .param("_csrf", token.group(1))
                .param("title", title)
                .param("summary", summary)
                .param("body", body);
        var cookies = form.getResponse().getCookies();
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return request;
    }

    private String refused(MockMultipartHttpServletRequestBuilder request) throws Exception {
        var result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).as("the form is refused").isEqualTo(422);
        return result.getResponse().getContentAsString();
    }

    private static List<String> invalid(String html) {
        var found = new ArrayList<String>();
        var field = INVALID.matcher(html);
        while (field.find()) {
            found.add(field.group(1));
        }
        return found;
    }

    /** What the element the field names in its {@code aria-describedby} says. */
    private static String errorBeside(String html, String field) {
        var error =
                Pattern.compile("(?s)id=\"" + field + "-error\"[^>]*>(.*?)</p>").matcher(html);
        assertThat(error.find()).as("a message beside %s", field).isTrue();
        return error.group(1);
    }

    private void publish(String title) {
        transaction.executeWithoutResult(status -> {
            var now = OffsetDateTime.now();
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody("Text.");
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }
}
