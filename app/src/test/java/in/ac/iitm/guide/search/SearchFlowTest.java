package in.ac.iitm.guide.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FEAT-007 through the real page: rows committed through JPA, so they are indexed the way the
 * application indexes them, and the search answered by the configured analyzer rather than a stub.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchFlowTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-28T12:00:00+05:30");

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private List<SmartLifecycle> lifecycles;

    @AfterEach
    void clearTheDatabaseAndTheIndex() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
        // The rows went by JDBC, which the index does not see.
        purgeTheIndex();
    }

    /** English, Hindi and Tamil in one body, with combining signs and a nukta (NFR-003). */
    private static final String THREE_SCRIPTS =
            "Register at the hostel office. छात्रावास कार्यालय में पंजीकरण ज़रूरी है। விடுதி அலுவலகத்தில் பதிவு செய்யவும்.";

    @Test
    // trace:NFR-003
    void an_article_mixing_english_hindi_and_tamil_is_stored_and_read_back_unchanged() throws Exception {
        publish("Hostel registration", THREE_SCRIPTS);

        assertThat(jdbc.queryForObject("SELECT body FROM article", String.class))
                .isEqualTo(THREE_SCRIPTS);
        assertThat(mockMvc.perform(get("/articles/hostel-registration"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .contains("छात्रावास कार्यालय में पंजीकरण ज़रूरी है।")
                .contains("விடுதி அலுவலகத்தில் பதிவு செய்யவும்.");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"पंजीकरण", "छात्रावास कार्यालय", "அலுவலகத்தில்", "விடுதி பதிவு"})
    // trace:NFR-003
    void a_query_in_hindi_or_tamil_finds_the_article_holding_those_words(String query) throws Exception {
        publish("Hostel registration", THREE_SCRIPTS);
        publish("Bank account", "Open an account at the campus branch.");

        var page = mockMvc.perform(get("/search").param("q", query))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page).contains("1 result for").contains(">Hostel registration<");
    }

    @Test
    // trace:FR-007
    void thirty_matches_show_the_first_twenty_with_the_total() throws Exception {
        for (var i = 0; i < 30; i++) {
            publish("Hostel guide " + i, "Rooms and mess.");
        }

        var page = mockMvc.perform(get("/search").param("q", "hostel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page).contains("30 results for &quot;hostel&quot;");
        assertThat(Pattern.compile("<li class=\"search-hit\"")
                        .matcher(page)
                        .results()
                        .count())
                .isEqualTo(20);
    }

    @Test
    // trace:FR-007
    void a_published_article_whose_title_contains_a_word_from_the_query_appears_in_the_results() throws Exception {
        publish("Registering with FRRO", "Nothing else here.");

        assertThat(search("frro")).contains("href=\"/articles/registering-with-frro\"");
    }

    @Test
    // trace:FR-007
    void a_published_article_whose_body_contains_a_word_from_the_query_appears_in_the_results() throws Exception {
        publish("Your first week", "Visit the FRRO office before the end of the week.");

        assertThat(search("frro")).contains("href=\"/articles/your-first-week\"");
    }

    @Test
    // trace:FR-007
    void a_published_article_whose_tags_contain_a_word_from_the_query_appears_in_the_results() throws Exception {
        publish("Opening a bank account", "Bring your passport.", "paperwork");

        assertThat(search("paperwork")).contains("href=\"/articles/opening-a-bank-account\"");
    }

    @Test
    // trace:FR-007
    void only_articles_holding_every_word_of_the_query_appear() throws Exception {
        publish("Registering with FRRO", "Registration takes fourteen days.");
        publish("Connecting to the campus Wi-Fi", "Registration of your laptop comes first.");
        publish("Banks and ATMs", "Bring your FRRO papers.");

        var page = search("FRRO registration");

        assertThat(page)
                .contains("href=\"/articles/registering-with-frro\"")
                .doesNotContain("/articles/connecting-to-the-campus-wi-fi")
                .doesNotContain("/articles/banks-and-atms");
    }

    @Test
    // trace:FR-007
    void the_words_may_be_spread_over_the_title_the_text_and_the_tags() throws Exception {
        publish("Hostel rules", "Lights out at eleven.", "laundry");

        assertThat(search("hostel eleven laundry")).contains("href=\"/articles/hostel-rules\"");
    }

    @Test
    // trace:FR-007
    void markup_and_query_syntax_typed_into_the_box_find_nothing() throws Exception {
        publish("Hostel life", "The hostel has a siren for fire drills.");
        publish("Hostel mess", "The mess opens at seven.");

        assertThat(search("<script>alert(1)</script>")).contains("No article holds all of these words");
        assertThat(search("hostel\" OR 1=1 --")).contains("No article holds all of these words");
    }

    @Test
    // trace:FR-007
    void when_no_article_holds_every_word_the_page_says_so_and_links_to_the_tags() throws Exception {
        publish("Hostel mess", "The mess opens at seven.");

        var page = search("hostel snorkelling");

        assertThat(page)
                .contains("No article holds all of these words")
                .doesNotContain("/articles/hostel-mess")
                .contains("href=\"/tags\"");
    }

    @Test
    // trace:FR-007
    void a_result_shows_a_passage_of_the_text_with_the_words_marked() throws Exception {
        publish("Your first week", "Open a bank account, then visit the FRRO office before Friday.");

        assertThat(search("frro office")).contains("visit the <mark>FRRO</mark> <mark>office</mark> before Friday");
    }

    @Test
    // trace:FR-007
    void the_passage_is_plain_text_without_markdown_or_wiki_link_brackets() throws Exception {
        publish(
                "Your first week",
                "**Visit** the [[Registering with FRRO|FRRO]] page and [the office](https://frro.gov.in).");

        var page = search("visit office");

        assertThat(passage(page)).doesNotContain("**").doesNotContain("[[").doesNotContain("](");
        assertThat(passage(page)).contains("<mark>Visit</mark>").contains("the <mark>office</mark>");
    }

    @Test
    // trace:FR-007
    void html_in_the_text_is_escaped_in_the_passage() throws Exception {
        publish("Odd article", "Mention of <script>alert('x')</script> and the canteen.");

        assertThat(passage(search("canteen"))).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    // trace:FR-007
    void the_words_are_marked_in_the_title_too() throws Exception {
        publish("Registering with FRRO", "Fourteen days.");

        assertThat(search("frro")).contains("Registering with <mark>FRRO</mark>");
    }

    @Test
    // trace:FR-007
    void when_only_a_submission_not_yet_approved_matches_no_result_appears_for_it() throws Exception {
        publish("Campus map", "Where the buildings are.");
        submit("Snorkelling in Kovalam", SubmissionStatus.PENDING);

        var page = search("snorkelling");

        assertThat(page).doesNotContain("Snorkelling in Kovalam").contains("No articles matched");
    }

    @Test
    // trace:FR-007
    void when_only_a_rejected_submission_matches_no_result_appears_for_it() throws Exception {
        publish("Campus map", "Where the buildings are.");
        submit("Paragliding in Yelagiri", SubmissionStatus.REJECTED);

        var page = search("paragliding");

        assertThat(page).doesNotContain("Paragliding in Yelagiri").contains("No articles matched");
    }

    @Test
    // trace:FR-007
    void the_match_ignores_case_and_word_form() throws Exception {
        publish("FRRO registration", "Register within fourteen days.");

        assertThat(search("REGISTERING")).contains("href=\"/articles/frro-registration\"");
    }

    @Test
    // trace:FR-007
    void a_removed_article_does_not_appear() throws Exception {
        var removed = anArticle("Old bus timetable", "The shuttle ran every hour.");
        removed.setRemovedAt(NOW);
        save(removed);

        assertThat(search("shuttle"))
                .doesNotContain("/articles/old-bus-timetable")
                .contains("No articles matched");
    }

    @Test
    // trace:FR-007
    void a_result_matched_only_in_its_title_or_tags_shows_its_summary_and_its_tags() throws Exception {
        publish("Getting a SIM card", "Airtel and Jio both have shops on campus.", "phone");

        assertThat(search("phone")).contains("A summary of Getting a SIM card.").contains(">phone<");
    }

    @Test
    // trace:FR-007
    void the_query_is_kept_in_the_search_box() throws Exception {
        assertThat(search("hostel <mess>")).contains("value=\"hostel &lt;mess&gt;\"");
    }

    @Test
    // trace:FR-007
    void a_missing_query_answers_400() throws Exception {
        mockMvc.perform(get("/search")).andExpect(status().isBadRequest());
    }

    @Test
    // trace:FR-007
    void a_blank_query_answers_400() throws Exception {
        mockMvc.perform(get("/search").param("q", "   ")).andExpect(status().isBadRequest());
    }

    @Test
    // trace:FR-007
    void a_query_the_analyzer_reduces_to_nothing_answers_200_with_no_results() throws Exception {
        // Punctuation alone passes the blank check but leaves no term to search for.
        publish("Campus map", "Where the buildings are.");

        assertThat(search("?!")).contains("No articles matched");
    }

    @Test
    // trace:FR-007
    void an_empty_index_is_filled_from_the_database_at_start_up() throws Exception {
        // Other test classes share this context and may have left documents in its index.
        purgeTheIndex();
        insertByJdbc("Monsoon clothing", "Bring an umbrella.");

        startUp();

        assertThat(search("umbrella")).contains("href=\"/articles/monsoon-clothing\"");
    }

    @Test
    // trace:FR-007
    void an_index_that_is_not_empty_is_left_as_it_is_at_start_up() throws Exception {
        // Decided by the human, 28 Sep: the index is kept on disk between starts, and rebuilt only
        // when there is none. This article by JDBC stands for one the index has not seen.
        publish("Campus map", "Where the buildings are.");
        insertByJdbc("Monsoon clothing", "Bring an umbrella.");

        startUp();

        assertThat(search("umbrella")).doesNotContain("/articles/monsoon-clothing");
    }

    @Test
    // trace:FR-007
    void an_article_the_moderator_approves_is_found() throws Exception {
        submit("Cycling to Guindy", SubmissionStatus.PENDING);
        var number = jdbc.queryForObject(
                "SELECT submission_number FROM submission WHERE title = 'Cycling to Guindy'", String.class);

        var session = loggedIn();
        var review = mockMvc.perform(get("/moderate/submissions/" + number).session(session))
                .andExpect(status().isOk())
                .andReturn();
        mockMvc.perform(post("/moderate/submissions/" + number + "/approve")
                        .session(session)
                        .cookie(review.getResponse().getCookies())
                        .param("_csrf", csrf(review))
                        .param("summary", "A summary of Cycling to Guindy."))
                .andExpect(status().is3xxRedirection());

        assertThat(search("cycling")).contains("href=\"/articles/cycling-to-guindy\"");
    }

    @Test
    // trace:FR-007
    void query_syntax_typed_into_the_search_box_is_searched_as_words_not_obeyed() throws Exception {
        // Phase 4 edge case "injection attempt": the query is analysed as text, so Lucene's operators
        // — a trailing * for a prefix, a leading - to exclude — widen nothing.
        publish("Bank account", "Open one in the first week.");

        assertThat(search("acc*")).contains("No articles matched");
        assertThat(search("bank -account")).contains("href=\"/articles/bank-account\"");
    }

    @Test
    // trace:FR-007
    void a_query_of_more_than_fifty_words_answers_400() throws Exception {
        // Found by review, 28 Sep: a thousand distinct words, about 4 KB and so a valid URL, took
        // the query past Lucene's limit of 1024 clauses and answered 500.
        mockMvc.perform(get("/search").param("q", words(1000))).andExpect(status().isBadRequest());
    }

    @Test
    // trace:FR-007
    void words_joined_by_hyphens_count_as_separate_words() throws Exception {
        // Found by the second review, 28 Sep: counting only the spaces let a thousand hyphenated
        // words through as one, and the analyzer split them into a thousand terms again.
        mockMvc.perform(get("/search").param("q", words(1000).replace(' ', '-')))
                .andExpect(status().isBadRequest());
    }

    @Test
    // trace:FR-007
    void a_query_of_fifty_words_is_searched() throws Exception {
        publish("Campus map", "Where the buildings are.");

        mockMvc.perform(get("/search").param("q", words(49) + " buildings")).andExpect(status().isOk());
    }

    @Test
    // trace:FR-007
    void a_query_of_fifty_one_words_answers_400() throws Exception {
        mockMvc.perform(get("/search").param("q", words(51))).andExpect(status().isBadRequest());
    }

    /** The first result's passage, as the page shows it. */
    private static String passage(String page) {
        var matcher = Pattern.compile("<p class=\"search-hit-passage\">(.*?)</p>", Pattern.DOTALL)
                .matcher(page);
        assertThat(matcher.find()).as("the result shows a passage").isTrue();
        return matcher.group(1);
    }

    /** Distinct three-letter words, so no two of them reduce to one term. */
    private static String words(int count) {
        var words = new StringBuilder();
        for (int i = 0; i < count; i++) {
            words.append((char) ('a' + i / 676))
                    .append((char) ('a' + i / 26 % 26))
                    .append((char) ('a' + i % 26));
            words.append(' ');
        }
        return words.toString().strip();
    }

    private void startUp() throws Exception {
        for (var lifecycle : lifecycles) {
            if (lifecycle.getClass().getPackageName().startsWith(SearchFlowTest.class.getPackageName())) {
                lifecycle.start();
            }
        }
    }

    private void purgeTheIndex() {
        transaction.executeWithoutResult(
                status -> Search.session(entityManager).workspace().purge());
    }

    private void insertByJdbc(String title, String body) {
        jdbc.update(
                "INSERT INTO article (id, title, slug, summary, body, published_at, updated_at)"
                        + " VALUES (?, ?, ?, 's', ?, ?, ?)",
                UUID.randomUUID(),
                title,
                ArticleAddress.slugOf(title).orElseThrow(),
                body,
                NOW,
                NOW);
    }

    private MockHttpSession loggedIn() throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var result = mockMvc.perform(post("/moderate/login")
                        .session(session)
                        .cookie(form.getResponse().getCookies())
                        .param("_csrf", csrf(form))
                        .param("password", PASSWORD))
                .andReturn();
        assertThat(result.getResponse().getRedirectedUrl())
                .as("the login succeeded")
                .isEqualTo("/moderate/queue");
        return (MockHttpSession) result.getRequest().getSession();
    }

    private static String csrf(MvcResult page) throws Exception {
        var matcher = CSRF.matcher(page.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the page carries a CSRF token").isTrue();
        return matcher.group(1);
    }

    private String search(String query) throws Exception {
        return mockMvc.perform(get("/search").param("q", query))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void publish(String title, String body, String... tags) {
        save(anArticle(title, body), tags);
    }

    private Article anArticle(String title, String body) {
        var article = new Article();
        article.setTitle(title);
        article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
        article.setSummary("A summary of " + title + ".");
        article.setBody(body);
        article.setPublishedAt(NOW);
        article.setUpdatedAt(NOW);
        return article;
    }

    private void save(Article article, String... tagNames) {
        transaction.executeWithoutResult(status -> {
            var tags = new HashSet<Tag>();
            for (var name : tagNames) {
                var tag = new Tag();
                tag.setName(name);
                entityManager.persist(tag);
                tags.add(tag);
            }
            article.setTags(tags);
            entityManager.persist(article);
        });
    }

    /** Through JPA, as the application stores one, so an index that took submissions in would show it. */
    private void submit(String title, SubmissionStatus status) {
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
            entityManager.persist(submission);
        });
    }
}
