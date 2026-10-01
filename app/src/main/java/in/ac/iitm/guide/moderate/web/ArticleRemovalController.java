package in.ac.iitm.guide.moderate.web;

import in.ac.iitm.guide.moderate.internal.ArticleNotLiveException;
import in.ac.iitm.guide.moderate.internal.ArticleRemoval;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** FR-026's two routes, behind the moderator login like all of {@code /moderate/**}: ask, then remove. */
// trace:FR-026
@Controller
class ArticleRemovalController {

    private final ArticleRemoval removal;

    ArticleRemovalController(ArticleRemoval removal) {
        this.removal = removal;
    }

    @GetMapping("/moderate/articles/{address}/remove")
    String confirm(@PathVariable String address, Model model) {
        model.addAttribute("candidate", removal.candidate(address));
        model.addAttribute("address", address);
        return "moderate/RemoveArticle";
    }

    @PostMapping("/moderate/articles/{address}/remove")
    String remove(@PathVariable String address, RedirectAttributes redirect) {
        var title = removal.remove(address);
        redirect.addFlashAttribute(Done.ATTRIBUTE, new Done("Removed", title, null));
        return "redirect:/moderate/queue";
    }

    @ExceptionHandler(ArticleNotLiveException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() {
        return "error/404";
    }
}
