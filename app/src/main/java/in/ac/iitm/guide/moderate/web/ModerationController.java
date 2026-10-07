package in.ac.iitm.guide.moderate.web;

import in.ac.iitm.guide.moderate.internal.AlreadyDecidedException;
import in.ac.iitm.guide.moderate.internal.ApprovalConflictException;
import in.ac.iitm.guide.moderate.internal.ApprovalRefusedException;
import in.ac.iitm.guide.moderate.internal.ModerationService;
import in.ac.iitm.guide.moderate.internal.ModerationService.Review;
import in.ac.iitm.guide.moderate.internal.RejectionRefusedException;
import in.ac.iitm.guide.moderate.internal.SubmissionNotFoundException;
import in.ac.iitm.guide.moderate.persistence.ModerateArticleRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The routes of FEAT-006 (docs/architecture/ui-routes.md), all behind the moderator login. */
// trace:FR-014
// trace:FR-015
// trace:FR-017
// trace:FR-018
// trace:FR-019
@Controller
class ModerationController {

    private final ModerationService moderation;
    private final ModerateArticleRepository articles;
    private final Tags tags;
    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    ModerationController(ModerationService moderation, ModerateArticleRepository articles, Tags tags) {
        this.moderation = moderation;
        this.articles = articles;
        this.tags = tags;
    }

    @GetMapping("/moderate/queue")
    String queue(Model model) {
        model.addAttribute("entries", moderation.queue());
        return "moderate/ModerationQueue";
    }

    @GetMapping("/moderate/submissions/{number}")
    String review(@PathVariable String number, Model model) {
        return reviewPage(model, moderation.review(number), null, null);
    }

    @PostMapping("/moderate/submissions/{number}/approve")
    String approve(
            @PathVariable String number,
            @RequestParam(defaultValue = "") String summary,
            @RequestParam(defaultValue = "") List<String> tags,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        var filled = tags.stream().filter(tag -> !tag.isBlank()).toList();
        try {
            var published = moderation.approve(number, summary, filled);
            redirect.addFlashAttribute(Done.ATTRIBUTE, new Done("Published:", published.title(), published.path()));
            return "redirect:/moderate/queue";
        } catch (AlreadyDecidedException e) {
            response.setStatus(HttpStatus.CONFLICT.value());
            return reviewPage(model, moderation.review(number), null, null);
        } catch (ApprovalConflictException e) {
            response.setStatus(HttpStatus.CONFLICT.value());
            return reviewPage(model, moderation.review(number), e.getMessage(), new Settled(summary, filled));
        } catch (ApprovalRefusedException e) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return reviewPage(model, moderation.review(number), e.getMessage(), new Settled(summary, filled));
        }
    }

    @PostMapping("/moderate/submissions/{number}/reject")
    String reject(
            @PathVariable String number,
            @RequestParam(required = false) String reason,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        try {
            var title = moderation.reject(number, reason);
            redirect.addFlashAttribute(Done.ATTRIBUTE, new Done("Rejected", title, null));
            return "redirect:/moderate/queue";
        } catch (AlreadyDecidedException e) {
            response.setStatus(HttpStatus.CONFLICT.value());
            return reviewPage(model, moderation.review(number), null, null);
        } catch (RejectionRefusedException e) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("reason", reason);
            return reviewPage(model, moderation.review(number), e.getMessage(), null);
        }
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() {
        return "error/404";
    }

    /** What the moderator had typed, shown again when the approval was refused. */
    private record Settled(String summary, List<String> tags) {}

    private String reviewPage(Model model, Review review, String error, Settled settled) {
        var summary = settled == null ? review.summary() : settled.summary();
        var chosen = settled == null ? review.tags() : settled.tags();

        model.addAttribute("review", review);
        model.addAttribute("bodyHtml", renderer.render(review.body(), this::resolve));
        model.addAttribute("summary", summary);
        // Fix 3.7 (F-25): the tag field of the contributor's form (ADR-0022), the submission's tags chosen.
        model.addAttribute("chosenTags", chosen);
        model.addAttribute("tagChoices", Tags.choices(chosen, tags.inUse()));
        model.addAttribute("tagMost", Tags.MOST);
        model.addAttribute("error", error);
        model.addAttribute("reasonLimit", ModerationService.REASON_LIMIT);
        model.addAttribute("summaryLimit", Article.LONGEST_SUMMARY);
        return "moderate/SubmissionReview";
    }

    /** Which linked titles are live articles, so the moderator sees the red links a reader would. */
    private Map<String, String> resolve(Set<String> titles) {
        var slugOfTitle = new HashMap<String, String>();
        for (var title : titles) {
            ArticleAddress.slugOf(title).ifPresent(slug -> slugOfTitle.put(title, slug));
        }
        if (slugOfTitle.isEmpty()) {
            return Map.of();
        }
        var live = articles.findLiveSlugs(new HashSet<>(slugOfTitle.values()));
        var hrefs = new HashMap<String, String>();
        slugOfTitle.forEach((title, slug) -> {
            if (live.contains(slug)) {
                hrefs.put(title, ArticleAddress.pathOf(slug));
            }
        });
        return hrefs;
    }
}
