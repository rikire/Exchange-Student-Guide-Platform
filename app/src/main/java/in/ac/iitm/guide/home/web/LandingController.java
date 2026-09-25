package in.ac.iitm.guide.home.web;

import in.ac.iitm.guide.home.internal.LandingPageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** {@code GET /}: the landing page. Always {@code 200}, including a guide with no articles yet. */
// trace:FR-009
@Controller
class LandingController {

    private final LandingPageService landing;

    LandingController(LandingPageService landing) {
        this.landing = landing;
    }

    @GetMapping("/")
    String show(Model model) {
        model.addAttribute("landing", landing.build());
        return "home/Landing";
    }
}
