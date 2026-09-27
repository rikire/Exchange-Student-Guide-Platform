package in.ac.iitm.guide.contribute.web;

import in.ac.iitm.guide.contribute.internal.SubmissionRejectedException;
import in.ac.iitm.guide.contribute.internal.SubmissionService;
import in.ac.iitm.guide.contribute.internal.SubmissionService.Draft;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The routes of FEAT-005 (docs/architecture/ui-routes.md): the form, its two POSTs, the confirmation. */
// trace:FR-010
// trace:FR-011
@Controller
class SubmissionController {

    /** The form always offers this many tag fields, more when an article already has more tags. */
    private static final int TAG_FIELDS = 5;

    private final SubmissionService submissions;

    SubmissionController(SubmissionService submissions) {
        this.submissions = submissions;
    }

    @GetMapping("/submit")
    String newArticleForm(Model model) {
        return form(model, FormPage.forNewArticle(new Draft("", "", "", List.of())));
    }

    @PostMapping("/submissions")
    String submitNewArticle(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String summary,
            @RequestParam(defaultValue = "") String body,
            @RequestParam(defaultValue = "") List<String> tags,
            Model model,
            HttpServletResponse response) {
        var draft = new Draft(title, summary, body, filled(tags));
        try {
            return confirmation(submissions.submitNewArticle(draft));
        } catch (SubmissionRejectedException e) {
            var link = e.collision().map(slug -> ArticleAddress.pathOf(slug) + "/edit");
            var page = FormPage.forNewArticle(draft)
                    .refused(e.getMessage(), link.orElse(null), "Propose an edit to the existing article");
            return refused(model, response, page);
        }
    }

    @GetMapping("/articles/{address}/edit")
    String editForm(@PathVariable String address, Model model) {
        return form(model, FormPage.forEdit(address, submissions.draftOf(address)));
    }

    @PostMapping("/articles/{address}/edits")
    String submitEdit(
            @PathVariable String address,
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String summary,
            @RequestParam(defaultValue = "") String body,
            @RequestParam(defaultValue = "") List<String> tags,
            Model model,
            HttpServletResponse response) {
        var draft = new Draft(title, summary, body, filled(tags));
        try {
            return confirmation(submissions.submitEdit(address, draft));
        } catch (SubmissionRejectedException e) {
            var link = e.collision().map(ArticleAddress::pathOf);
            var page = FormPage.forEdit(address, draft).refused(e.getMessage(), link.orElse(null), "Open that article");
            return refused(model, response, page);
        }
    }

    @GetMapping("/submissions/{number}/confirmation")
    String confirmation(@PathVariable String number, Model model) {
        var issued = submissions.issued(number).orElseThrow(SubmissionNotFoundException::new);
        model.addAttribute("number", issued);
        return "contribute/SubmissionConfirmation";
    }

    /** The form's tag fields arrive whether or not they were filled in; an empty one is not a tag. */
    private static List<String> filled(List<String> tags) {
        return tags.stream().filter(tag -> !tag.isBlank()).toList();
    }

    private static String confirmation(String number) {
        return "redirect:/submissions/" + number + "/confirmation";
    }

    private static String form(Model model, FormPage page) {
        model.addAttribute("form", page);
        return "contribute/SubmissionForm";
    }

    private static String refused(Model model, HttpServletResponse response, FormPage page) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        return form(model, page);
    }

    /** What the form template shows. {@code tags} is padded with empty fields to fill in. */
    record FormPage(
            String heading,
            String action,
            String title,
            String summary,
            String body,
            List<String> tags,
            String error,
            String linkHref,
            String linkLabel) {

        static FormPage forNewArticle(Draft draft) {
            return of("Submit a new article", "/submissions", draft);
        }

        static FormPage forEdit(String address, Draft draft) {
            var path =
                    ArticleAddress.slugOf(address).map(ArticleAddress::pathOf).orElseThrow();
            return of("Propose an edit", path + "/edits", draft);
        }

        private static FormPage of(String heading, String action, Draft draft) {
            var fields = new ArrayList<>(draft.tags());
            do {
                fields.add("");
            } while (fields.size() < TAG_FIELDS);
            return new FormPage(
                    heading, action, draft.title(), draft.summary(), draft.body(), fields, null, null, null);
        }

        FormPage refused(String error, String linkHref, String linkLabel) {
            return new FormPage(heading, action, title, summary, body, tags, error, linkHref, linkLabel);
        }
    }

    /** A number that was never issued, or text that cannot be one: both look the same from outside. */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class SubmissionNotFoundException extends RuntimeException {}
}
