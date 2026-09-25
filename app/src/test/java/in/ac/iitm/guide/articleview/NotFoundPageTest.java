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
}
