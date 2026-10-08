package in.ac.iitm.guide.about.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** {@code GET /oge-team} and {@code GET /developers}: always {@code 200}, to anyone. */
// trace:FR-035
@Controller
class AboutController {

    @GetMapping("/oge-team")
    String ogeTeam() {
        return "about/OgeTeam";
    }

    @GetMapping("/developers")
    String developers() {
        return "about/Developers";
    }
}
