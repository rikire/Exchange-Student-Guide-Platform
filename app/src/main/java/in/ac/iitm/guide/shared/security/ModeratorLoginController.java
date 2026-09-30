package in.ac.iitm.guide.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ADR-0009's login: one shared password, compared with the BCrypt hash the server was started with.
 *
 * <p>A controller rather than Spring Security's {@code formLogin}, which needs a username this model
 * does not have and answers a wrong password with a redirect where the route contract says {@code
 * 401} on the form. The pieces that matter are still Spring Security's: the hash comparison, the
 * session id changed on login against fixation, the CSRF token replaced as {@code formLogin} replaces
 * it, and the session-held security context the filter chain reads on the next request.
 *
 * <p>Failed attempts are limited per client address ({@link FailedLoginLimit}); past the limit even
 * the right password answers {@code 429}, so a guess cannot learn it is right.
 */
// trace:FR-014
// trace:NFR-005
@Controller
class ModeratorLoginController {

    private static final Logger log = LoggerFactory.getLogger(ModeratorLoginController.class);

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ChangeSessionIdAuthenticationStrategy fixation = new ChangeSessionIdAuthenticationStrategy();
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final CsrfAuthenticationStrategy csrf;
    private final FailedLoginLimit failures;
    private final String passwordHash;

    ModeratorLoginController(
            @Value("${guide.admin.password-hash}") String passwordHash,
            CsrfTokenRepository csrfTokens,
            FailedLoginLimit failures) {
        this.passwordHash = passwordHash;
        this.csrf = new CsrfAuthenticationStrategy(csrfTokens);
        this.failures = failures;
    }

    @GetMapping(WebSecurity.LOGIN)
    String form() {
        return "shared/security/AdminLogin";
    }

    @PostMapping(WebSecurity.LOGIN)
    String logIn(
            @RequestParam(defaultValue = "") String password,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        var address = request.getRemoteAddr();
        var attempt = failures.take(address);
        if (!attempt.isConsumed()) {
            return tooMany(response, model, Duration.ofNanos(attempt.getNanosToWaitForRefill()));
        }
        if (!matches(password)) {
            log.warn("Failed moderator login");
            if (attempt.getRemainingTokens() == 0) {
                log.warn("Moderator login rate limit reached for one address; its next attempt waits for the window");
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            model.addAttribute("error", true);
            return "shared/security/AdminLogin";
        }
        failures.giveBack(address);

        var moderator = UsernamePasswordAuthenticationToken.authenticated(
                "moderator", null, List.of(new SimpleGrantedAuthority("ROLE_" + WebSecurity.MODERATOR)));
        request.getSession();
        fixation.onAuthentication(moderator, request, response);
        csrf.onAuthentication(moderator, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(moderator);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return "redirect:/moderate/queue";
    }

    /** Whole minutes, rounded up, in the message; whole seconds, rounded up, in {@code Retry-After}. */
    private static String tooMany(HttpServletResponse response, Model model, Duration wait) {
        var seconds = Math.max(1, (wait.toNanos() + 999_999_999) / 1_000_000_000);
        var minutes = (seconds + 59) / 60;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        model.addAttribute("tooMany", minutes + (minutes == 1 ? " minute" : " minutes"));
        return "shared/security/AdminLogin";
    }

    /** With no hash configured nothing matches: an unset variable must not open the gate. */
    private boolean matches(String password) {
        return !passwordHash.isBlank() && encoder.matches(password, passwordHash);
    }
}
