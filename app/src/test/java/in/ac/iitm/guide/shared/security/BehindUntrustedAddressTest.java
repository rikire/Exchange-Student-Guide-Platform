package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * DEBT-014: {@code X-Forwarded-For} from an address that is not the proxy is ignored. Otherwise a
 * guesser would send a new address with every attempt and never meet the limit.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.forward-headers-strategy=native",
            "server.tomcat.remoteip.internal-proxies=10\\\\.255\\\\.255\\\\.254",
            "guide.admin.failed-login-limit.requests=10",
            "guide.admin.failed-login-limit.per=15m"
        })
class BehindUntrustedAddressTest {

    private static final String PASSWORD = "a long random passphrase for the office";

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    @Test
    // trace:NFR-005
    void a_forwarded_address_from_anyone_but_the_proxy_does_not_reset_the_limit() throws Exception {
        for (var i = 1; i <= 10; i++) {
            assertThat(new ProxyLogin(port, "203.0.113." + (20 + i))
                            .logIn("not the password")
                            .statusCode())
                    .as("attempt %d", i)
                    .isEqualTo(401);
        }

        var next = new ProxyLogin(port, "203.0.113.99").logIn(PASSWORD);

        assertThat(next.statusCode()).isEqualTo(429);
    }
}
