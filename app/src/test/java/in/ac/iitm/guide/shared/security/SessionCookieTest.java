package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * security.md, "The admin area": the moderator's session cookie is {@code HttpOnly} and
 * {@code SameSite=Lax}. Measured on a real server, since MockMvc never writes the container's cookie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCookieTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    @Test
    // trace:FR-014
    void the_session_cookie_a_login_sets_is_http_only_and_same_site_lax() throws Exception {
        // The CSRF token is kept in a cookie, so the session begins only when the login succeeds.
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        var form = client.send(
                HttpRequest.newBuilder(uri("/moderate/login")).build(), HttpResponse.BodyHandlers.ofString());
        var token = CSRF.matcher(form.body());
        assertThat(token.find()).as("the login form carries a CSRF token").isTrue();

        var login = client.send(
                HttpRequest.newBuilder(uri("/moderate/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "_csrf=" + encoded(token.group(1)) + "&password=" + encoded(PASSWORD)))
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(login.statusCode()).as("the login succeeded").isEqualTo(302);
        assertThat(login.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("JSESSIONID=")
                .containsIgnoringCase("HttpOnly")
                .containsIgnoringCase("SameSite=Lax"));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
