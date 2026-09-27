package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class WebSecurityTest {

    @Autowired
    private ApplicationContext context;

    @Test
    // trace:FR-010
    void no_user_account_exists_so_no_password_is_generated_and_logged() {
        // Spring Boot creates a "user" with a random password and logs it at start-up whenever no
        // account source is declared; found on the Docker stand on 27 Sep. Nothing here logs in
        // until ADR-0009's moderator login, and a password in a log line is what security.md forbids.
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }
}
