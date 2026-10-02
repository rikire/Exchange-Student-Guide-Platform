package in.ac.iitm.guide.contribute.web;

import in.ac.iitm.guide.contribute.Draft;
import in.ac.iitm.guide.contribute.internal.ArticleNotPublishedException;
import in.ac.iitm.guide.contribute.internal.SubmissionService;
import in.ac.iitm.guide.contribute.web.SubmissionController.FormPage;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.taxonomy.Tags;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * A form larger than the container accepts ({@code spring.servlet.multipart.*}) is refused while it
 * is read, before any controller has it, so the answer cannot be a controller's: it is here, with the
 * form again and no field kept, since none was read (ui-routes.md, {@code 413}).
 */
// trace:FR-010
// trace:FR-011
@ControllerAdvice
class UploadTooLargeAdvice {

    private static final Pattern EDIT = Pattern.compile("^/articles/([^/]+)/edits$");

    private final SubmissionService submissions;
    private final MediaAssets media;
    private final Tags tags;

    UploadTooLargeAdvice(SubmissionService submissions, MediaAssets media, Tags tags) {
        this.submissions = submissions;
        this.media = media;
        this.tags = tags;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String tooLarge(HttpServletRequest request, Model model) {
        var error = "The attachment is larger than the guide accepts, so nothing was saved. " + media.accepted()
                + " Fill in the form again with a smaller file.";
        var form = formFor(request.getRequestURI()).refused(error, null, null);
        model.addAttribute("form", form);
        model.addAttribute("accepted", media.accepted());
        // The tag list as SubmissionController gives it (fix 3.6, ADR-0022).
        model.addAttribute("tagChoices", form.tagChoices(tags.inUse()));
        model.addAttribute("tagMost", Tags.MOST);
        return "contribute/SubmissionForm";
    }

    private FormPage formFor(String path) {
        var edit = EDIT.matcher(path);
        if (edit.matches()) {
            try {
                var editing = submissions.editing(edit.group(1));
                return FormPage.forEdit(
                        edit.group(1), editing.article(), editing.draft().title(), editing.draft());
            } catch (ArticleNotPublishedException e) {
                // The article went while the file was on its way; the empty form is still somewhere to go.
            }
        }
        return FormPage.forNewArticle(new Draft("", "", "", List.of()));
    }
}
