package in.ac.iitm.guide.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Whether the page is shown to a signed-in moderator, for the shared header (fix 2.3, F-24): their
 * header leads to the queue, the homepage list, the guide and Log out instead of the public links.
 * Here beside the login, since every slice's pages use the one header.
 */
// trace:FR-014
@ControllerAdvice
class ModeratorFrame {

    @ModelAttribute("moderator")
    boolean moderator(HttpServletRequest request) {
        return request.isUserInRole(WebSecurity.MODERATOR);
    }
}
