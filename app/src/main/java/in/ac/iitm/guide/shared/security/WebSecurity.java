package in.ac.iitm.guide.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * What Spring Security does here: the CSRF token on every state-changing form
 * (docs/architecture/security.md), which refuses a POST without it with {@code 403}; its default
 * response headers; and ADR-0009's gate, which sends every {@code /moderate/**} request without a
 * moderator session to the login page. Every other route is public. Its logout ends the moderator's
 * session on {@code POST /moderate/logout}, which the CSRF token guards like any other form, and
 * clears the token with it.
 *
 * <p>The token is kept in a cookie, not the session, so a form still submits after the 30-minute
 * session has expired: an article can take longer than that to write, and the refused POST lost the
 * text (decided by the human on 27 Sep). The cookie stays {@code HttpOnly}: the editor's preview
 * takes the token from the form's hidden field, not from the cookie.
 *
 * <p>ADR-0013's Content-Security-Policy goes on every response: scripts, styles, fonts and media only
 * from the site itself, nothing inline, and no framing. It is the second layer behind ADR-0001's
 * escaping of article text.
 */
// trace:FR-010
// trace:FR-014
// trace:FR-027
@Configuration
class WebSecurity {

    static final String MODERATOR = "MODERATOR";
    static final String LOGIN = "/moderate/login";
    static final String LOGOUT = "/moderate/logout";

    static final String POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self';"
            + " font-src 'self'; connect-src 'self'; media-src 'self'; object-src 'none'; base-uri 'none';"
            + " form-action 'self'; frame-ancestors 'none'";

    /** One repository for the filter chain and for the login, which replaces the token through it. */
    @Bean
    CsrfTokenRepository csrfTokens() {
        return new CookieCsrfTokenRepository();
    }

    @Bean
    SecurityFilterChain publicRoutes(HttpSecurity http, CsrfTokenRepository csrfTokens) throws Exception {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(LOGIN)
                        .permitAll()
                        .requestMatchers("/moderate/**")
                        .hasRole(MODERATOR)
                        .anyRequest()
                        .permitAll())
                .exceptionHandling(
                        exceptions -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN)))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .logout(logout -> logout.logoutUrl(LOGOUT).logoutSuccessUrl(LOGIN + "?logout"))
                .headers(headers -> headers.contentSecurityPolicy(policy -> policy.policyDirectives(POLICY)));
        return http.build();
    }
}
