package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.guide.media.MediaTestFiles;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FEAT-005 through the real controller, service, H2 and migrations, with Spring Security's real CSRF
 * filter: every POST carries the token its form was rendered with, taken from the page the way a
 * browser would, so a form that forgot the token fails here.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionFlowTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Path MEDIA = MediaTestFiles.newRoot();

    @DynamicPropertySource
    static void media(DynamicPropertyRegistry registry) {
        MediaTestFiles.smallLimits(registry, MEDIA);
    }

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
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM report");
        jdbc.execute("DELETE FROM revision");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @AfterEach
    void clearTheMediaRoot() throws IOException {
        MediaTestFiles.empty(MEDIA);
    }

    @Test
    // trace:FR-010
    void a_new_article_submitted_through_the_form_is_pending_in_the_queue_and_its_number_is_shown() throws Exception {
        var result = submitNew("Getting a SIM card", "Where to buy one.", "Take your passport.", "SIM", " Phone ");

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        var confirmation = result.getResponse().getRedirectedUrl();
        assertThat(confirmation).matches("/submissions/SUB-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}/confirmation");
        var number = confirmation.split("/")[2];

        var row = onlySubmission();
        assertThat(row)
                .containsEntry("SUBMISSION_NUMBER", number)
                .containsEntry("TYPE", "NEW_ARTICLE")
                .containsEntry("STATUS", "PENDING")
                .containsEntry("TITLE", "Getting a SIM card")
                .containsEntry("SUMMARY", "Where to buy one.")
                .containsEntry("BODY", "Take your passport.")
                .containsEntry("TARGET_ARTICLE_ID", null);
        assertThat(suggestedTags()).containsExactlyInAnyOrder("sim", "phone");

        assertThat(page(confirmation)).contains(number);
    }

    @Test
    // trace:FR-010
    void a_title_matching_an_existing_articles_case_insensitively_is_refused_with_a_link_to_propose_an_edit()
            throws Exception {
        publish("Hostel Life");

        var result = submitNew("HOSTEL LIFE", "Rooms.", "My text.");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        var page = result.getResponse().getContentAsString();
        assertThat(page)
                .as("the link to propose an edit to the existing article")
                .contains("href=\"/articles/hostel-life/edit\"");
        assertThat(page)
                .as("the form again, with what was typed, so the title can be changed")
                .contains("value=\"HOSTEL LIFE\"")
                .contains("My text.");
        assertThat(submissionCount()).isZero();
    }

    @Test
    // trace:FR-010
    void a_title_whose_address_is_taken_by_another_title_is_refused_the_same_way() throws Exception {
        publish("Fees & Payments");

        var result = submitNew("Fees Payments", "Fees.", "Text.");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString()).contains("href=\"/articles/fees-payments/edit\"");
        assertThat(submissionCount()).isZero();
    }

    @Test
    // trace:FR-010
    void a_title_taken_by_a_removed_article_is_refused_without_a_link_that_would_not_resolve() throws Exception {
        var removed = anArticle("Old Hostel Rules");
        removed.setRemovedAt(OffsetDateTime.now());
        save(removed);

        var result = submitNew("Old Hostel Rules", "Rules.", "Text.");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("/articles/old-hostel-rules");
        assertThat(submissionCount()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    // trace:FR-010
    @CsvSource(
            delimiter = '|',
            value = {
                "a title with no letters or digits | '!!! ???' | Summary. | Body.",
                "a blank title                      | '   '     | Summary. | Body.",
                "a blank summary                    | Title     | '  '     | Body.",
                "a blank body                       | Title     | Summary. | '  '"
            })
    void a_submission_that_cannot_be_published_is_refused_on_the_form(
            String why, String title, String summary, String body) throws Exception {
        var result = submitNew(title, summary, body);

        assertThat(result.getResponse().getStatus()).as(why).isEqualTo(422);
        assertThat(submissionCount()).isZero();
    }

    @Test
    // trace:FR-010
    void a_title_of_255_characters_is_accepted_and_one_of_256_is_refused() throws Exception {
        assertThat(submitNew("t".repeat(256), "S.", "B.").getResponse().getStatus())
                .isEqualTo(422);
        assertThat(submitNew("t".repeat(255), "S.", "B.").getResponse().getStatus())
                .isEqualTo(302);
    }

    @Test
    // trace:FR-010
    void a_tag_too_long_to_store_is_refused_on_the_form_and_nothing_is_stored() throws Exception {
        var result = submitNew("Title", "Summary.", "Body.", "x".repeat(65));

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString()).contains("64");
        assertThat(submissionCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tag", Integer.class))
                .isZero();
    }

    @Test
    // trace:FR-011
    void a_proposed_edit_is_pending_in_the_queue_tied_to_its_article_and_its_number_is_shown() throws Exception {
        var article = publish("Hostel Life");

        var form = page("/articles/hostel-life/edit");
        assertThat(form).as("the form is filled in from the article").contains("value=\"Hostel Life\"");

        var result = submitEdit("hostel-life", "Hostel Life", "A summary.", "Rooms are shared.", "hostel");

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        var row = onlySubmission();
        assertThat(row)
                .containsEntry("TYPE", "EDIT")
                .containsEntry("STATUS", "PENDING")
                .containsEntry("TARGET_ARTICLE_ID", article.getId())
                .containsEntry("BODY", "Rooms are shared.");
        assertThat(page(result.getResponse().getRedirectedUrl())).contains((String) row.get("SUBMISSION_NUMBER"));
        assertThat(page("/articles/hostel-life"))
                .as("the article is unchanged until a moderator approves")
                .contains("The original text.")
                .doesNotContain("Rooms are shared.");
    }

    @Test
    // trace:FR-011
    void an_edits_new_title_matching_a_different_articles_is_refused_with_a_link_to_that_article() throws Exception {
        publish("Hostel Life");
        publish("Mess Food");

        var result = submitEdit("mess-food", "HOSTEL life", "A summary.", "Text.");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        var page = result.getResponse().getContentAsString();
        assertThat(page).contains("href=\"/articles/hostel-life\"").contains("value=\"HOSTEL life\"");
        assertThat(submissionCount()).isZero();
    }

    @Test
    // trace:FR-011
    void an_edit_may_keep_its_own_title_or_change_its_capitalisation() throws Exception {
        publish("Hostel Life");

        var result = submitEdit("hostel-life", "HOSTEL LIFE", "A summary.", "Text.");

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
    }

    @Test
    // trace:FR-011
    void an_edit_to_an_article_that_is_not_published_is_refused() throws Exception {
        var removed = anArticle("Old Hostel Rules");
        removed.setRemovedAt(OffsetDateTime.now());
        save(removed);

        mockMvc.perform(get("/articles/never-written/edit")).andExpect(status().isNotFound());
        mockMvc.perform(get("/articles/old-hostel-rules/edit")).andExpect(status().isNotFound());
        assertThat(submitEdit("never-written", "Title", "S.", "B.")
                        .getResponse()
                        .getStatus())
                .isEqualTo(404);
        assertThat(submitEdit("old-hostel-rules", "Old Hostel Rules", "S.", "B.")
                        .getResponse()
                        .getStatus())
                .isEqualTo(404);
        assertThat(submissionCount()).isZero();
    }

    @Test
    // trace:FR-003
    void wiki_link_markup_in_a_submitted_body_is_stored_unchanged() throws Exception {
        var body = "See [[Hostel Life]], [[ FRRO ]] and [[Fees & Payments|fees]].\n\n  Indented [[x]]  ";

        submitNew("Arrival checklist", "What to do first.", body);

        assertThat(onlySubmission()).containsEntry("BODY", body);
    }

    @Test
    // trace:FR-010
    void a_submission_is_reachable_from_no_public_page() throws Exception {
        var result = submitNew("A Secret Title", "Secret summary.", "Secret body.");

        // Without these two, a submission refused on the way in would pass the checks below.
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(onlySubmission()).containsEntry("STATUS", "PENDING");
        assertThat(page("/")).doesNotContain("A Secret Title");
        mockMvc.perform(get("/articles/a-secret-title")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-010
    void a_number_is_found_ignoring_case_and_hyphens_and_one_never_issued_is_not_found() throws Exception {
        var confirmation = submitNew("Title", "Summary.", "Body.").getResponse().getRedirectedUrl();
        var number = confirmation.split("/")[2];
        var typed = number.toLowerCase().replace("-", "");

        assertThat(page("/submissions/" + typed + "/confirmation")).contains(number);
        mockMvc.perform(get("/submissions/SUB-0000-0000-0000/confirmation")).andExpect(status().isNotFound());
        mockMvc.perform(get("/submissions/not-a-number/confirmation")).andExpect(status().isNotFound());
    }

    @Test
    // trace:FR-010
    void a_post_without_a_csrf_token_is_refused_and_stores_nothing() throws Exception {
        mockMvc.perform(post("/submissions")
                        .param("title", "Title")
                        .param("summary", "Summary.")
                        .param("body", "Body."))
                .andExpect(status().isForbidden());

        assertThat(submissionCount()).isZero();
    }

    @Test
    // trace:FR-010
    void a_form_still_submits_after_its_session_has_expired() throws Exception {
        // A session lasts 30 minutes and an article can take longer to write. With the token kept in
        // the session, the POST was refused with 403 and the text was lost (review of 27 Sep).
        var form = mockMvc.perform(get("/submit")).andExpect(status().isOk()).andReturn();
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();

        var result = mockMvc.perform(withCookiesOf(
                        form,
                        post("/submissions")
                                .session(new MockHttpSession())
                                .param("_csrf", matcher.group(1))
                                .param("title", "Written slowly")
                                .param("summary", "Summary.")
                                .param("body", "Body.")))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(submissionCount()).isEqualTo(1);
    }

    @Test
    // trace:FR-011
    void an_article_page_links_to_proposing_an_edit_and_every_page_to_submitting_one() throws Exception {
        publish("Hostel Life");

        assertThat(page("/articles/hostel-life")).contains("href=\"/articles/hostel-life/edit\"");
        assertThat(page("/")).contains("href=\"/submit\"");
    }

    @Test
    // trace:FR-010
    void the_form_sends_files_and_offers_one_attachment() throws Exception {
        var form = page("/submit");

        assertThat(form).contains("enctype=\"multipart/form-data\"");
        assertThat(form).contains("type=\"file\"").contains("name=\"attachment\"");
    }

    @Test
    // trace:FR-010
    void a_new_article_submitted_with_a_photo_is_pending_in_the_queue_and_the_photo_is_stored_with_it()
            throws Exception {
        var photo = file("sim-booth.jpg", MediaTestFiles.jpeg(30, 20));

        var result = submitWith("/submit", "/submissions", photo);

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(onlySubmission()).containsEntry("STATUS", "PENDING");
        assertThat(onlyAsset())
                .containsEntry("SUBMISSION_ID", onlySubmission().get("ID"))
                .containsEntry("ORIGINAL_NAME", "sim-booth.jpg");
    }

    @Test
    // trace:FR-011
    void an_edit_submitted_with_a_photo_is_pending_in_the_queue_and_the_photo_is_stored_with_it() throws Exception {
        publish("Registering with FRRO");
        var photo = file("frro-form.jpg", MediaTestFiles.jpeg(30, 20));

        var result = submitWith("/articles/registering-with-frro/edit", "/articles/registering-with-frro/edits", photo);

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(onlySubmission()).containsEntry("TYPE", "EDIT").containsEntry("STATUS", "PENDING");
        assertThat(onlyAsset()).containsEntry("SUBMISSION_ID", onlySubmission().get("ID"));
    }

    @Test
    // trace:FR-010
    void a_new_article_with_an_attachment_over_its_limit_is_refused_with_a_message_and_nothing_is_stored()
            throws Exception {
        var large = file("big.pdf", MediaTestFiles.pdf(16 * 1024 + 1));

        var result = submitWith("/submit", "/submissions", large);

        assertRefusedKeepingTheText(result, "larger than 16 KB");
    }

    @Test
    // trace:FR-011
    void an_edit_with_an_attachment_over_its_limit_is_refused_with_a_message_and_nothing_is_stored() throws Exception {
        publish("Registering with FRRO");
        var large = file("big.pdf", MediaTestFiles.pdf(16 * 1024 + 1));

        var result = submitWith("/articles/registering-with-frro/edit", "/articles/registering-with-frro/edits", large);

        assertRefusedKeepingTheText(result, "larger than 16 KB");
    }

    @Test
    // trace:FR-010
    void a_new_article_with_an_attachment_not_of_an_accepted_type_is_refused_with_a_message() throws Exception {
        var page = file("form.jpg", "<!DOCTYPE html><html><body>hi</body></html>".getBytes());

        var result = submitWith("/submit", "/submissions", page);

        assertRefusedKeepingTheText(result, "not an accepted type");
    }

    @Test
    // trace:FR-011
    void an_edit_with_an_attachment_not_of_an_accepted_type_is_refused_with_a_message() throws Exception {
        publish("Registering with FRRO");
        var page = file("form.jpg", "<!DOCTYPE html><html><body>hi</body></html>".getBytes());

        var result = submitWith("/articles/registering-with-frro/edit", "/articles/registering-with-frro/edits", page);

        assertRefusedKeepingTheText(result, "not an accepted type");
    }

    @Test
    // trace:FR-010
    void a_form_sent_with_no_file_chosen_is_submitted_without_an_attachment() throws Exception {
        // A browser sends the file field even when nothing was chosen: an empty part with no name.
        var nothing = file("", new byte[0]);

        var result = submitWith("/submit", "/submissions", nothing);

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class))
                .isZero();
    }

    private void assertRefusedKeepingTheText(MvcResult result, String message) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString())
                .contains(message)
                .as("the form again, with what was typed")
                .contains("value=\"With a file\"")
                .contains("The text that goes with it.");
        assertThat(submissionCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class))
                .isZero();
        assertThat(MediaTestFiles.files(MEDIA)).isEmpty();
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("attachment", name, "application/octet-stream", bytes);
    }

    /** As {@link #submit}, sent as {@code multipart/form-data} with one file. */
    private MvcResult submitWith(String formPath, String action, MockMultipartFile attachment) throws Exception {
        var form = mockMvc.perform(get(formPath)).andExpect(status().isOk()).andReturn();
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find())
                .as("the form at %s carries a CSRF token", formPath)
                .isTrue();
        var session = (MockHttpSession) form.getRequest().getSession();

        var request = multipart(action)
                .file(attachment)
                .session(session)
                .param("_csrf", matcher.group(1))
                .param("title", "With a file")
                .param("summary", "A summary.")
                .param("body", "The text that goes with it.");
        return mockMvc.perform(withCookiesOf(form, request)).andReturn();
    }

    private Map<String, Object> onlyAsset() {
        return jdbc.queryForMap("SELECT * FROM media_asset");
    }

    private MvcResult submitNew(String title, String summary, String body, String... tags) throws Exception {
        return submit("/submit", "/submissions", title, summary, body, tags);
    }

    private MvcResult submitEdit(String address, String title, String summary, String body, String... tags)
            throws Exception {
        // The token comes from the empty form when the edit form itself does not resolve: a 404 on the
        // POST has to be the article's doing, not a missing token's.
        var formPath = "/articles/" + address + "/edit";
        var formStatus =
                mockMvc.perform(get(formPath)).andReturn().getResponse().getStatus();
        return submit(
                formStatus == 200 ? formPath : "/submit",
                "/articles/" + address + "/edits",
                title,
                summary,
                body,
                tags);
    }

    /** Renders the form, then posts to {@code action} with the token and session the form came with. */
    private MvcResult submit(String formPath, String action, String title, String summary, String body, String... tags)
            throws Exception {
        var form = mockMvc.perform(get(formPath)).andExpect(status().isOk()).andReturn();
        var matcher = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(matcher.find())
                .as("the form at %s carries a CSRF token", formPath)
                .isTrue();
        var session = (MockHttpSession) form.getRequest().getSession();

        var request = post(action)
                .session(session)
                .param("_csrf", matcher.group(1))
                .param("title", title)
                .param("summary", summary)
                .param("body", body);
        for (var tag : tags) {
            request.param("tags", tag);
        }
        return mockMvc.perform(withCookiesOf(form, request)).andReturn();
    }

    /** Sends back the cookies the form was served with, as a browser does. */
    private static MockHttpServletRequestBuilder withCookiesOf(MvcResult form, MockHttpServletRequestBuilder request) {
        var cookies = form.getResponse().getCookies();
        return cookies.length == 0 ? request : request.cookie(cookies);
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private Map<String, Object> onlySubmission() {
        return jdbc.queryForMap("SELECT * FROM submission");
    }

    private int submissionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM submission", Integer.class);
    }

    private List<String> suggestedTags() {
        return jdbc.queryForList("SELECT t.name FROM submission_tag st JOIN tag t ON t.id = st.tag_id", String.class);
    }

    private Article publish(String title) {
        var article = anArticle(title);
        save(article);
        return article;
    }

    private Article anArticle(String title) {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary.");
        article.setBody("The original text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(Set.of());
        return article;
    }

    private void save(Article article) {
        transaction.executeWithoutResult(status -> entityManager.persist(article));
    }
}
