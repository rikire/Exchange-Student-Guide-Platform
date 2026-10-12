package in.ac.iitm.guide.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Any error without a page of its own is the site's page, not Tomcat's: the demo rehearsal's failed
 * approval showed "HTTP Status 500 – Internal Server Error" on a white page (5 Oct). Through a real
 * server, since MockMvc does not dispatch to the error page. No requirement names it; security.md's
 * rule "no secret in an error page" is the one it checks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ErrorPageTest.Failing.class)
class ErrorPageTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void an_unexpected_failure_is_the_sites_own_page_with_a_way_back_and_no_detail() {
        var headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_HTML));

        var response = http.exchange("/test-only/fails", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .contains("href=\"/css/site.css\"")
                .contains("Something went wrong")
                .contains("href=\"/\"")
                .doesNotContain("the detail only the log may hold")
                .doesNotContain("IllegalStateException");
    }

    @TestConfiguration
    @RestController
    static class Failing {

        @GetMapping("/test-only/fails")
        String fails() {
            throw new IllegalStateException("the detail only the log may hold");
        }
    }
}
