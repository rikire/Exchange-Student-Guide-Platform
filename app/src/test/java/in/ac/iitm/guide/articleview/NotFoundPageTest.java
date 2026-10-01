package in.ac.iitm.guide.articleview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Through a real server, not MockMvc: MockMvc stops at the status and never dispatches to the error
 * page, so a broken or missing {@code error/404.html} would pass every controller test. What a
 * browser sees for an unknown address is the page in the shared frame, not a JSON body.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotFoundPageTest {

    @Autowired
    private TestRestTemplate http;

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
        var response =
                http.exchange(path, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        return response.getBody();
    }
}
