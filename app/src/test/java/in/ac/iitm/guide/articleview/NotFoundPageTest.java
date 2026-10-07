package in.ac.iitm.guide.articleview;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Through a real server, not MockMvc: MockMvc stops at the status and never dispatches to the error
 * page, so a broken or missing {@code error/404.html} would pass every controller test. What a
 * browser sees for an unknown address is the page in the shared frame, not a JSON body.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotFoundPageTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    /** Through the entity manager, so the search index forgets them too. */
    @AfterEach
    void clearTheArticles() {
        transaction.executeWithoutResult(status -> entityManager
                .createQuery("SELECT a FROM Article a", Article.class)
                .getResultList()
                .forEach(entityManager::remove));
    }

    @Test
    // trace:FR-001
    void every_not_found_page_offers_the_search_box_and_the_way_back() {
        var page = notFound("/no-such-page");

        assertThat(page)
                .containsPattern("<form[^>]*action=\"/search\"")
                .containsPattern("class=\"button-quiet button-back\" href=\"/\"");
    }

    @Test
    // trace:FR-001
    void a_missing_article_suggests_published_titles_close_to_its_address() {
        publish("Registering with FRRO");
        publish("Opening a bank account");

        var page = notFound("/articles/registring-frro");

        assertThat(page)
                .contains("Did you mean")
                .contains("<a href=\"/articles/registering-with-frro\">Registering with FRRO</a>")
                .doesNotContain("Opening a bank account");
    }

    @Test
    // trace:FR-005
    void a_red_links_address_invites_the_reader_to_write_the_article_with_its_title() {
        var page = notFound("/articles/sim-card-registration?title=SIM%20card%20registration");

        assertThat(page)
                .contains("This article doesn't exist yet")
                .contains("SIM card registration")
                .contains("href=\"/submit?title=SIM%20card%20registration\"")
                .contains(">Create this article</a>");
    }

    @Test
    // trace:FR-005
    void the_invitation_is_a_card_naming_the_missing_title_in_the_red_links_colour() {
        var page = notFound("/articles/sim-card-registration?title=SIM%20card%20registration");

        // Walkthrough-fixes 3.9, N6: the card of docs/design/screens/RedlinkInvite.html.
        assertThat(page)
                .containsPattern("<section class=\"invite-card\"[^>]*>")
                .contains("<p class=\"invite-target\">SIM card registration</p>");
    }

    @Test
    // trace:FR-005
    void an_address_typed_without_a_red_links_title_is_not_an_invitation() {
        assertThat(notFound("/articles/sim-card-registration")).doesNotContain("Create this article");
    }

    @Test
    // trace:FR-001
    void a_missing_article_close_to_no_title_suggests_nothing() {
        publish("Opening a bank account");

        assertThat(notFound("/articles/qwxz")).doesNotContain("Did you mean");
    }

    private void publish(String title) {
        transaction.executeWithoutResult(status -> {
            var article = new Article();
            article.setTitle(title);
            article.setSlug(ArticleAddress.slugOf(title).orElseThrow());
            article.setSummary("A summary.");
            article.setBody("Text.");
            article.setPublishedAt(OffsetDateTime.now());
            article.setUpdatedAt(OffsetDateTime.now());
            article.setTags(Set.of());
            entityManager.persist(article);
        });
    }

    @Test
    // trace:FR-001
    void a_browser_asking_for_an_unknown_article_gets_the_404_page_in_the_shared_frame() {
        var headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.TEXT_HTML));

        var response = http.exchange(
                "/articles/no-such-article",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("We could not find that page").contains("href=\"/css/site.css\"");
    }

    @Test
    // trace:FR-001
    void an_unknown_article_is_named_as_an_article() {
        assertThat(notFound("/articles/no-such-article")).contains("There is no published article at this address");
    }

    @Test
    // trace:FR-008
    void an_unknown_tag_is_named_as_a_tag() {
        assertThat(notFound("/tags/no-such-tag"))
                .contains("We could not find that tag")
                .contains("No published article carries this tag")
                .contains("href=\"/tags\"")
                .doesNotContain("no published article at this address");
    }

    @Test
    // trace:FR-001
    void any_other_unknown_address_is_named_as_a_page() {
        assertThat(notFound("/no-such-page"))
                .contains("There is no page at this address")
                .doesNotContain("published article");
    }

    private String notFound(String path) {
        var headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.TEXT_HTML));
        // A URI, not a template string: the template would encode a "%20" in the path a second time.
        var uri = java.net.URI.create(http.getRootUri() + path);
        var response =
                http.exchange(uri, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        return response.getBody();
    }
}
