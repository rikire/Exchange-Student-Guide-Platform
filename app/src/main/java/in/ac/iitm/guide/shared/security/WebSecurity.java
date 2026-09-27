package in.ac.iitm.guide.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * What Spring Security does here today: the CSRF token on every state-changing form
 * (docs/architecture/security.md), which refuses a POST without it with {@code 403}, and its default
 * response headers. Every route is public; the moderator login of ADR-0009 gates {@code /moderate/**}
 * when {@code moderate} is built.
 *
 * <p>The token is kept in a cookie, not the session, so a form still submits after the 30-minute
 * session has expired: an article can take longer than that to write, and the refused POST lost the
 * text (decided by the human on 27 Sep). The cookie stays {@code HttpOnly}, since no script reads it.
 */
// trace:FR-010
@Configuration
class WebSecurity {

    @Bean
    SecurityFilterChain publicRoutes(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()));
        return http.build();
    }
}
