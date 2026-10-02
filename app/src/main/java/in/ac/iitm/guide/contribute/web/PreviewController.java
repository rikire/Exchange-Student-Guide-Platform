package in.ac.iitm.guide.contribute.web;

import in.ac.iitm.guide.contribute.internal.BodyPreview;
import in.ac.iitm.guide.contribute.internal.BodyPreview.PreviewTooLongException;
import in.ac.iitm.guide.contribute.internal.ContributionLimits;
import in.ac.iitm.guide.shared.web.RetryAfter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /contribute/preview}: the editor's preview pane asks for the body's HTML as the
 * contributor pauses typing (FR-027). The answer is a fragment, not a page, refusals included: the
 * editor's script puts whatever comes back into the pane. NFR-005 limits it per client address.
 */
// trace:FR-027
// trace:NFR-005
@RestController
class PreviewController {

    private final BodyPreview preview;
    private final ContributionLimits limits;

    PreviewController(BodyPreview preview, ContributionLimits limits) {
        this.preview = preview;
        this.limits = limits;
    }

    @PostMapping(path = "/contribute/preview", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> preview(@RequestParam(defaultValue = "") String body, HttpServletRequest request) {
        var wait = limits.takePreview(request.getRemoteAddr());
        if (wait.isPresent()) {
            var retry = new RetryAfter(wait.get());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(retry.seconds()))
                    .body("<p>Too many previews from your network. The preview returns in " + retry.seconds()
                            + " seconds; your text is kept.</p>");
        }
        return ResponseEntity.ok(preview.render(body));
    }

    @ExceptionHandler(PreviewTooLongException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String tooLong() {
        return "<p>The text is longer than the preview shows (" + BodyPreview.LONGEST_BODY + " characters).</p>";
    }
}
