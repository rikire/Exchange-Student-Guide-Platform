package in.ac.iitm.guide.contribute.web;

import in.ac.iitm.guide.contribute.internal.ArticleNotPublishedException;
import in.ac.iitm.guide.contribute.internal.ArticleRemovedWhileEditingException;
import in.ac.iitm.guide.contribute.internal.BodyPreview;
import in.ac.iitm.guide.contribute.internal.ContributionLimits;
import in.ac.iitm.guide.contribute.internal.SubmissionRejectedException;
import in.ac.iitm.guide.contribute.internal.SubmissionService;
import in.ac.iitm.guide.contribute.internal.SubmissionService.Draft;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.Upload;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * The routes of FEAT-005 (docs/architecture/ui-routes.md): the form, its two POSTs, the confirmation;
 * and FEAT-012's status lookup. Both POSTs count against NFR-005's one limit per client address, and
 * only a submission accepted keeps its count.
 */
// trace:FR-010
// trace:FR-011
// trace:FR-012
// trace:NFR-005
@Controller
class SubmissionController {

    /** The form always offers this many tag fields, more when an article already has more tags. */
    private static final int TAG_FIELDS = 5;

    private final SubmissionService submissions;
    private final MediaAssets media;
    private final ContributionLimits limits;

    SubmissionController(SubmissionService submissions, MediaAssets media, ContributionLimits limits) {
        this.submissions = submissions;
        this.media = media;
        this.limits = limits;
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
            @RequestParam(required = false) MultipartFile attachment,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        var draft = new Draft(title, summary, body, filled(tags));
        var address = request.getRemoteAddr();
        var wait = limits.takeSubmission(address);
        if (wait.isPresent()) {
            return tooMany(model, response, FormPage.forNewArticle(draft), wait.get());
        }
        var accepted = false;
        try {
            var number = submissions.submitNewArticle(draft, chosen(attachment));
            accepted = true;
            return confirmation(number);
        } catch (SubmissionRejectedException e) {
            var link = e.collision().map(slug -> ArticleAddress.pathOf(slug) + "/edit");
            var page = FormPage.forNewArticle(draft)
                    .refused(e.getMessage(), link.orElse(null), "Propose an edit to the existing article");
            return refused(model, response, page);
        } finally {
            if (!accepted) {
                limits.giveBackSubmission(address);
            }
        }
    }

    @GetMapping("/articles/{address}/edit")
    String editForm(@PathVariable String address, Model model) {
        var editing = submissions.editing(address);
        return form(model, FormPage.forEdit(address, editing.article(), editing.draft()));
    }

    @PostMapping("/articles/{address}/edits")
    String submitEdit(
            @PathVariable String address,
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String summary,
            @RequestParam(defaultValue = "") String body,
            @RequestParam(defaultValue = "") List<String> tags,
            @RequestParam(required = false) MultipartFile attachment,
            @RequestParam(required = false) UUID article,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        var draft = new Draft(title, summary, body, filled(tags));
        // An address no article can have is answered before the limit, which would show its form.
        ArticleAddress.slugOf(address).orElseThrow(() -> new ArticleNotPublishedException(address));
        var client = request.getRemoteAddr();
        var wait = limits.takeSubmission(client);
        if (wait.isPresent()) {
            return tooMany(model, response, FormPage.forEdit(address, article, draft), wait.get());
        }
        var accepted = false;
        try {
            var number = submissions.submitEdit(address, article, draft, chosen(attachment));
            accepted = true;
            return confirmation(number);
        } catch (SubmissionRejectedException e) {
            var link = e.collision().map(ArticleAddress::pathOf);
            var page = FormPage.forEdit(address, article, draft)
                    .refused(e.getMessage(), link.orElse(null), "Open that article");
            return refused(model, response, page);
        } catch (ArticleRemovedWhileEditingException e) {
            response.setStatus(HttpStatus.CONFLICT.value());
            return form(
                    model,
                    FormPage.forEdit(address, article, draft)
                            .refused(
                                    "This article was removed while you were editing it, so the edit cannot be sent."
                                            + " Your text is still below; copy it if you want to keep it.",
                                    null,
                                    null));
        } finally {
            if (!accepted) {
                limits.giveBackSubmission(client);
            }
        }
    }

    @GetMapping("/submissions/{number}/confirmation")
    String confirmation(@PathVariable String number, Model model) {
        var issued = submissions.issued(number).orElseThrow(SubmissionNotFoundException::new);
        model.addAttribute("number", issued);
        return "contribute/SubmissionConfirmation";
    }

    /** Without a number, or with a blank one, the page is only the form to type one into. */
    @GetMapping("/submissions/status")
    String status(@RequestParam(required = false) String number, Model model, HttpServletResponse response) {
        model.addAttribute("typed", number);
        if (number == null || number.isBlank()) {
            return "contribute/SubmissionStatus";
        }
        var found = submissions.statusOf(number);
        if (found.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("notFound", true);
        } else {
            model.addAttribute("found", found.get());
        }
        return "contribute/SubmissionStatus";
    }

    /**
     * A browser sends the file field when nothing was chosen, as an empty part with no name; that is
     * no attachment. An empty file that has a name was chosen, and {@code media} refuses it.
     */
    private static Optional<Upload> chosen(MultipartFile attachment) {
        if (attachment == null || (attachment.isEmpty() && !StringUtils.hasText(attachment.getOriginalFilename()))) {
            return Optional.empty();
        }
        return Optional.of(new Upload(attachment.getOriginalFilename(), attachment.getSize(), attachment));
    }

    /** The form's tag fields arrive whether or not they were filled in; an empty one is not a tag. */
    private static List<String> filled(List<String> tags) {
        return tags.stream().filter(tag -> !tag.isBlank()).toList();
    }

    private static String confirmation(String number) {
        return "redirect:/submissions/" + number + "/confirmation";
    }

    private String form(Model model, FormPage page) {
        model.addAttribute("form", page);
        model.addAttribute("accepted", media.accepted());
        model.addAttribute("summaryLimit", Article.LONGEST_SUMMARY);
        model.addAttribute("bodyLimit", BodyPreview.LONGEST_BODY);
        return "contribute/SubmissionForm";
    }

    private String refused(Model model, HttpServletResponse response, FormPage page) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        return form(model, page);
    }

    /** NFR-005's refusal: the form again with what was typed, and when to send it (ADR-0019). */
    private String tooMany(Model model, HttpServletResponse response, FormPage page, Duration wait) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        var retry = new RetryAfter(wait);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retry.seconds()));
        var minutes = retry.minutes();
        var error = "Too many submissions from your network. Please try again in " + minutes
                + (minutes == 1 ? " minute." : " minutes.") + " Your text is still below.";
        return form(model, page.refused(error, null, null));
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
            String linkLabel,
            UUID article) {

        static FormPage forNewArticle(Draft draft) {
            return of("Submit a new article", "/submissions", draft, null);
        }

        /** @param article the article the form was opened for, sent back so DEBT-009's 409 can be told apart */
        static FormPage forEdit(String address, UUID article, Draft draft) {
            var path =
                    ArticleAddress.slugOf(address).map(ArticleAddress::pathOf).orElseThrow();
            return of("Propose an edit", path + "/edits", draft, article);
        }

        private static FormPage of(String heading, String action, Draft draft, UUID article) {
            var fields = new ArrayList<>(draft.tags());
            do {
                fields.add("");
            } while (fields.size() < TAG_FIELDS);
            return new FormPage(
                    heading, action, draft.title(), draft.summary(), draft.body(), fields, null, null, null, article);
        }

        FormPage refused(String error, String linkHref, String linkLabel) {
            return new FormPage(heading, action, title, summary, body, tags, error, linkHref, linkLabel, article);
        }
    }

    /** A number that was never issued, or text that cannot be one: both look the same from outside. */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class SubmissionNotFoundException extends RuntimeException {}
}
