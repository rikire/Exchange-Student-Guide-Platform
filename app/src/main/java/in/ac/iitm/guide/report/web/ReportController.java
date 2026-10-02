package in.ac.iitm.guide.report.web;

import in.ac.iitm.guide.report.internal.ReportService;
import in.ac.iitm.guide.shared.web.RetryAfter;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * FR-021's form and its POST, open to anyone with the page's CSRF token; FR-022's inbox and closing,
 * under {@code /moderate/}, which WebSecurity keeps to the signed-in moderator.
 */
// trace:FR-021
// trace:FR-022
@Controller
class ReportController {

    static final String NOTICE = "notice";

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/articles/{address}/report")
    String form(@PathVariable String address, Model model) {
        var article = reports.reportable(address).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return form(model, article.getTitle(), article.getSlug(), "", null);
    }

    @PostMapping("/articles/{address}/reports")
    String report(
            @PathVariable String address,
            @RequestParam(defaultValue = "") String message,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        var article = reports.reportable(address).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (message.isBlank()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return form(model, article.getTitle(), article.getSlug(), message, "Say what is wrong with the article.");
        }
        if (message.strip().length() > ReportService.MESSAGE_LIMIT) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return form(
                    model,
                    article.getTitle(),
                    article.getSlug(),
                    message,
                    "At most " + ReportService.MESSAGE_LIMIT + " characters, please.");
        }
        var wait = reports.take(request.getRemoteAddr());
        if (wait.isPresent()) {
            var retry = new RetryAfter(wait.get());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retry.seconds()));
            return form(
                    model,
                    article.getTitle(),
                    article.getSlug(),
                    message,
                    "Too many reports from your network. Please try again in " + retry.minutes() + " minutes.");
        }
        reports.report(article, message);
        redirect.addFlashAttribute(NOTICE, "Thanks — OGE will look at it.");
        return "redirect:" + ArticleAddress.pathOf(article.getSlug());
    }

    @GetMapping("/moderate/reports")
    String inbox(Model model) {
        model.addAttribute("entries", reports.inbox());
        return "report/ReportInbox";
    }

    @PostMapping("/moderate/reports/{id}/close")
    String close(@PathVariable UUID id, RedirectAttributes redirect) {
        if (!reports.close(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirect.addFlashAttribute(NOTICE, "Report closed.");
        return "redirect:/moderate/reports";
    }

    private static String form(Model model, String title, String slug, String message, String error) {
        model.addAttribute("title", title);
        model.addAttribute("articlePath", ArticleAddress.pathOf(slug));
        model.addAttribute("action", ArticleAddress.pathOf(slug) + "/reports");
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        model.addAttribute("messageLimit", ReportService.MESSAGE_LIMIT);
        return "report/ReportForm";
    }
}
