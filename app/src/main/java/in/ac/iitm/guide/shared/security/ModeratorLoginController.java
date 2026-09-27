package in.ac.iitm.guide.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
 * session id changed on login against fixation, and the session-held security context the filter
 * chain reads on the next request.
 */
// trace:FR-014
// TODO(DEBT-011): failed attempts are logged, not rate limited, until NFR-005.
// TODO(DEBT-013): the CSRF token is not replaced on login, as formLogin would do.
@Controller
class ModeratorLoginController {

    private static final Logger log = LoggerFactory.getLogger(ModeratorLoginController.class);

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ChangeSessionIdAuthenticationStrategy fixation = new ChangeSessionIdAuthenticationStrategy();
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final String passwordHash;

    ModeratorLoginController(@Value("${guide.admin.password-hash}") String passwordHash) {
        this.passwordHash = passwordHash;
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
        if (!matches(password)) {
            log.warn("Failed moderator login");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            model.addAttribute("error", true);
            return "shared/security/AdminLogin";
        }

        var moderator = UsernamePasswordAuthenticationToken.authenticated(
                "moderator", null, List.of(new SimpleGrantedAuthority("ROLE_" + WebSecurity.MODERATOR)));
        request.getSession();
        fixation.onAuthentication(moderator, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(moderator);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return "redirect:/moderate/queue";
    }

    /** With no hash configured nothing matches: an unset variable must not open the gate. */
    private boolean matches(String password) {
        return !passwordHash.isBlank() && encoder.matches(password, passwordHash);
    }
}
