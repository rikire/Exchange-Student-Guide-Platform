package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * DEBT-014: behind IITM's proxy every connection comes from the proxy. With the forwarded headers
 * trusted from that proxy alone, the limit on failed logins is per visitor again, and the redirect
 * after signing in keeps the visitor on https. Here the proxy is this machine (127.0.0.1).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.forward-headers-strategy=native",
            "server.tomcat.remoteip.internal-proxies=127\\\\.0\\\\.0\\\\.1|0:0:0:0:0:0:0:1",
            "guide.admin.failed-login-limit.requests=10",
            "guide.admin.failed-login-limit.per=15m"
        })
class BehindTrustedProxyTest {

    private static final String PASSWORD = "a long random passphrase for the office";

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    @Test
    // trace:NFR-005
    void ten_failed_logins_from_one_visitor_leave_another_behind_the_same_proxy_able_to_sign_in() throws Exception {
        var guesser = new ProxyLogin(port, "203.0.113.5");
        for (var i = 1; i <= 10; i++) {
            assertThat(guesser.logIn("not the password").statusCode())
                    .as("attempt %d", i)
                    .isEqualTo(401);
        }
        assertThat(guesser.logIn("not the password").statusCode()).isEqualTo(429);

        var moderator = new ProxyLogin(port, "203.0.113.6").logIn(PASSWORD);

        assertThat(moderator.statusCode()).isEqualTo(302);
    }

    @Test
    // trace:FR-014
    void signing_in_through_an_https_proxy_redirects_to_https() throws Exception {
        var response = new ProxyLogin(port, "203.0.113.7").logIn(PASSWORD);

        assertThat(response.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).startsWith("https://"));
    }
}
