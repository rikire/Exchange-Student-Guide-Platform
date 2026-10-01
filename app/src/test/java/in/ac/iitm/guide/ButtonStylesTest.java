package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Walkthrough fix 3.2 (F-27): every action is one of the four buttons of
 * docs/design/screens/Buttons.html, so it reads as an action. Read from the templates, where the
 * class is written, rather than from rendered pages, most of which need fixtures and a session.
 */
// trace:NFR-007
class ButtonStylesTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @ParameterizedTest(name = "{1} in {0} is {2}")
    @CsvSource({
        "articleview/Article.html, Propose an edit, button-primary",
        "articleview/Article.html, Save as PDF, button-secondary",
        "articleview/Article.html, Remove this article, button-danger",
        "contribute/SubmissionForm.html, Submit for review, button-primary",
        "contribute/SubmissionForm.html, Cancel, button-back",
        "contribute/SubmissionConfirmation.html, Check its status, button-secondary",
        "contribute/SubmissionConfirmation.html, Copy, button-secondary",
        "contribute/SubmissionConfirmation.html, Back to the guide, button-back",
        "media/Attachments.html, Download, button-secondary",
        "shared/security/AdminLogin.html, Sign in, button-primary",
        "shared/security/AdminLogin.html, Back to the guide, button-back",
        "shared/security/LogOut.html, Log out, button-quiet",
        "moderate/SubmissionReview.html, Approve &amp; publish, button-primary",
        "moderate/SubmissionReview.html, Reject, button-danger",
        "moderate/RemoveArticle.html, Remove, button-danger",
        "moderate/RemoveArticle.html, Cancel, button-back",
        "moderate/ArticleAdmin.html, Pin, button-secondary",
        "moderate/ArticleAdmin.html, Move up, button-secondary",
    })
    void each_action_wears_its_button(String template, String label, String kind) throws IOException {
        var text = Files.readString(TEMPLATES.resolve(template));
        var element = Pattern.compile(
                        "<(a|button)\\b[^>]*\\bclass=\"([^\"]*)\"[^>]*>\\s*" + Pattern.quote(label) + "\\s*</\\1>")
                .matcher(text);

        assertThat(element.find())
                .as("an <a> or <button> with a class, reading \"%s\"", label)
                .isTrue();
        assertThat(element.group(2).split("\\s+")).as("its classes").contains(kind);
    }
}
