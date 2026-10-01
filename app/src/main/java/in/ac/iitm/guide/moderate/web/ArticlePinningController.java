package in.ac.iitm.guide.moderate.web;

import in.ac.iitm.guide.moderate.internal.ArticleNotLiveException;
import in.ac.iitm.guide.moderate.internal.ArticlePinning;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/** FR-025's screen and its four actions, behind the moderator login like all of {@code /moderate/**}. */
// trace:FR-025
@Controller
class ArticlePinningController {

    private static final String BACK = "redirect:/moderate/articles";

    private final ArticlePinning pinning;

    ArticlePinningController(ArticlePinning pinning) {
        this.pinning = pinning;
    }

    @GetMapping("/moderate/articles")
    String screen(@RequestParam(defaultValue = "1") int page, Model model) {
        if (page < 1) {
            throw new ArticleNotLiveException("page " + page);
        }
        model.addAttribute("screen", pinning.screen(page));
        return "moderate/ArticleAdmin";
    }

    @PostMapping("/moderate/articles/{address}/pin")
    String pin(@PathVariable String address) {
        pinning.pin(address);
        return BACK;
    }

    @PostMapping("/moderate/articles/{address}/unpin")
    String unpin(@PathVariable String address) {
        pinning.unpin(address);
        return BACK;
    }

    @PostMapping("/moderate/articles/{address}/up")
    String up(@PathVariable String address) {
        pinning.moveUp(address);
        return BACK;
    }

    @PostMapping("/moderate/articles/{address}/down")
    String down(@PathVariable String address) {
        pinning.moveDown(address);
        return BACK;
    }

    @ExceptionHandler(ArticleNotLiveException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() {
        return "error/404";
    }
}
