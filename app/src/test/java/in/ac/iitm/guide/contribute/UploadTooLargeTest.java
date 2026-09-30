package in.ac.iitm.guide.contribute;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.media.MediaTestFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Through a real server, not MockMvc: the container refuses a request over its multipart limit while
 * reading it, before any controller or filter has the form, and MockMvc has no container. The limit
 * is made small here so a test file crosses it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadTooLargeTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Path MEDIA = MediaTestFiles.ROOT;

    @DynamicPropertySource
    static void smallContainerLimit(DynamicPropertyRegistry registry) {
        registry.add("spring.servlet.multipart.max-file-size", () -> "4KB");
        registry.add("spring.servlet.multipart.max-request-size", () -> "8KB");
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * The accepted upload is a real submission with a photo. Under {@code -P postgres} every test
     * context shares one database, so left here it breaks the next class's cleanup (DEBT-022).
     */
    @AfterEach
    void clearTheDatabaseAndTheMediaRoot() throws IOException {
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        MediaTestFiles.empty(MEDIA);
    }

    @Test
    // trace:FR-010
    void a_request_larger_than_the_container_accepts_answers_413_with_the_form_and_the_error() {
        var response = submitWith(MediaTestFiles.pdf(20 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody())
                .contains("larger than")
                .contains("enctype=\"multipart/form-data\"")
                .contains("href=\"/css/site.css\"");
    }

    @Test
    // trace:FR-010
    void a_request_the_container_accepts_still_reaches_the_form_with_its_csrf_token() {
        // The token travels in the multipart body; if the security filter could not read it there,
        // every upload would be refused with 403. The client follows the redirect to the confirmation.
        var response = submitWith(MediaTestFiles.jpeg(10, 10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Submission received");
    }

    private org.springframework.http.ResponseEntity<String> submitWith(byte[] file) {
        var form = http.getForEntity("/submit", String.class);
        var matcher = CSRF.matcher(form.getBody());
        assertThat(matcher.find()).as("the form carries a CSRF token").isTrue();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.TEXT_HTML));
        headers.put(HttpHeaders.COOKIE, cookies(form.getHeaders()));
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("_csrf", matcher.group(1));
        body.add("title", "With a file " + System.nanoTime());
        body.add("summary", "A summary.");
        body.add("body", "The text.");
        body.add("attachment", new ByteArrayResource(file) {
            @Override
            public String getFilename() {
                return "file.bin";
            }
        });
        return http.postForEntity("/submissions", new HttpEntity<>(body, headers), String.class);
    }

    /** The cookies the form was served with, sent back as a browser does. */
    private static List<String> cookies(HttpHeaders served) {
        return served.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .toList();
    }
}
