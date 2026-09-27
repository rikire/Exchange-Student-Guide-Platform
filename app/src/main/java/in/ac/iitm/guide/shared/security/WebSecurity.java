package in.ac.iitm.guide.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * What Spring Security does here: the CSRF token on every state-changing form
 * (docs/architecture/security.md), which refuses a POST without it with {@code 403}; its default
 * response headers; and ADR-0009's gate, which sends every {@code /moderate/**} request without a
 * moderator session to the login page. Every other route is public.
 *
 * <p>The token is kept in a cookie, not the session, so a form still submits after the 30-minute
 * session has expired: an article can take longer than that to write, and the refused POST lost the
 * text (decided by the human on 27 Sep). The cookie stays {@code HttpOnly}, since no script reads it.
 */
// trace:FR-010
// trace:FR-014
@Configuration
class WebSecurity {

    static final String MODERATOR = "MODERATOR";
    static final String LOGIN = "/moderate/login";

    @Bean
    SecurityFilterChain publicRoutes(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(LOGIN)
                        .permitAll()
                        .requestMatchers("/moderate/**")
                        .hasRole(MODERATOR)
                        .anyRequest()
                        .permitAll())
                .exceptionHandling(
                        exceptions -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN)))
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()));
        return http.build();
    }
}
