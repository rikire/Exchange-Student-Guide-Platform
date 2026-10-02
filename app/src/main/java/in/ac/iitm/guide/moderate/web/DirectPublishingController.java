package in.ac.iitm.guide.moderate.web;

import in.ac.iitm.guide.contribute.Draft;
import in.ac.iitm.guide.contribute.SubmissionRejectedException;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.Upload;
import in.ac.iitm.guide.moderate.internal.DirectPublishing;
import in.ac.iitm.guide.moderate.internal.ModerationService.Published;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * FR-023 and FR-024's four routes, behind the moderator login like all of {@code /moderate/**}: the
 * moderator writes or edits an article and it is live at once. A refusal shows the form again with
 * what was typed: {@code 409} for a title another article has, {@code 422} for anything else.
 */
// trace:FR-023
// trace:FR-024
@Controller
class DirectPublishingController {

    /** As many tag fields as the contributor's form offers. */
    private static final int TAG_FIELDS = 5;

    private final DirectPublishing publishing;
    private final MediaAssets media;

    DirectPublishingController(DirectPublishing publishing, MediaAssets media) {
        this.publishing = publishing;
        this.media = media;
    }

    @GetMapping("/moderate/write")
    String newArticleForm(Model model) {
        return form(model, FormPage.forNewArticle(new Draft("", "", "", List.of())));
    }

    @PostMapping("/moderate/articles")
    String publishNew(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String summary,
            @RequestParam(defaultValue = "") String body,
            @RequestParam(defaultValue = "") List<String> tags,
            @RequestParam(required = false) MultipartFile attachment,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        var draft = new Draft(title, summary, body, filled(tags));
        return published(
                () -> publishing.publishNew(draft, chosen(attachment)),
                () -> FormPage.forNewArticle(draft),
                model,
                response,
                redirect);
    }

    @GetMapping("/moderate/articles/{address}/edit")
    String editForm(@PathVariable String address, Model model) {
        return form(model, FormPage.forEdit(address, publishing.draftOf(address)));
    }

    @PostMapping("/moderate/articles/{address}/edits")
    String publishEdit(
            @PathVariable String address,
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String summary,
            @RequestParam(defaultValue = "") String body,
            @RequestParam(defaultValue = "") List<String> tags,
            @RequestParam(required = false) MultipartFile attachment,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        var draft = new Draft(title, summary, body, filled(tags));
        return published(
                () -> publishing.publishEdit(address, draft, chosen(attachment)),
                () -> FormPage.forEdit(address, draft),
                model,
                response,
                redirect);
    }

    private String published(
            Supplier<Published> publish,
            Supplier<FormPage> page,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        try {
            var article = publish.get();
            redirect.addFlashAttribute("notice", "Published.");
            return "redirect:" + article.path();
        } catch (SubmissionRejectedException e) {
            response.setStatus(e.titleTaken() ? HttpStatus.CONFLICT.value() : HttpStatus.UNPROCESSABLE_ENTITY.value());
            return form(model, page.get().refused(e.getMessage()));
        }
    }

    /** As the contributor's form: the empty part a browser sends when no file was chosen is no attachment. */
    private static Optional<Upload> chosen(MultipartFile attachment) {
        if (attachment == null || (attachment.isEmpty() && !StringUtils.hasText(attachment.getOriginalFilename()))) {
            return Optional.empty();
        }
        return Optional.of(new Upload(attachment.getOriginalFilename(), attachment.getSize(), attachment));
    }

    private static List<String> filled(List<String> tags) {
        return tags.stream().filter(tag -> !tag.isBlank()).toList();
    }

    private String form(Model model, FormPage page) {
        model.addAttribute("form", page);
        model.addAttribute("accepted", media.accepted());
        model.addAttribute("summaryLimit", Article.LONGEST_SUMMARY);
        model.addAttribute("bodyLimit", Draft.LONGEST_BODY);
        return "moderate/PublishForm";
    }

    /** What the form template shows. {@code tags} is padded with empty fields to fill in. */
    record FormPage(
            String heading, String action, String title, String summary, String body, List<String> tags, String error) {

        static FormPage forNewArticle(Draft draft) {
            return of("Write an article", "/moderate/articles", draft);
        }

        /** Only for an address that has an article: a refused edit was refused after the article was found. */
        static FormPage forEdit(String address, Draft draft) {
            var slug = ArticleAddress.slugOf(address).orElseThrow();
            return of("Edit an article", "/moderate/articles/" + slug + "/edits", draft);
        }

        private static FormPage of(String heading, String action, Draft draft) {
            var fields = new ArrayList<>(draft.tags());
            do {
                fields.add("");
            } while (fields.size() < TAG_FIELDS);
            return new FormPage(heading, action, draft.title(), draft.summary(), draft.body(), fields, null);
        }

        FormPage refused(String error) {
            return new FormPage(heading, action, title, summary, body, tags, error);
        }
    }
}
