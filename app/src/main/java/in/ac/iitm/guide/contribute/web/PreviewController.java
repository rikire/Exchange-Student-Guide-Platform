package in.ac.iitm.guide.contribute.web;

import in.ac.iitm.guide.contribute.internal.BodyPreview;
import in.ac.iitm.guide.contribute.internal.BodyPreview.PreviewTooLongException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /contribute/preview}: the editor's preview pane asks for the body's HTML as the
 * contributor pauses typing (FR-027). The answer is a fragment, not a page.
 */
// trace:FR-027
@RestController
class PreviewController {

    private final BodyPreview preview;

    PreviewController(BodyPreview preview) {
        this.preview = preview;
    }

    @PostMapping(path = "/contribute/preview", produces = MediaType.TEXT_HTML_VALUE)
    String preview(@RequestParam(defaultValue = "") String body) {
        return preview.render(body);
    }

    @ExceptionHandler(PreviewTooLongException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String tooLong() {
        return "<p>The text is longer than the preview shows (" + BodyPreview.LONGEST_BODY + " characters).</p>";
    }
}
