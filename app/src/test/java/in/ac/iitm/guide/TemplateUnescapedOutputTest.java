package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * security.md, "Article content": {@code th:utext} is allowed only for what the Markdown converter
 * produced, and carries a comment saying so. Every other output escapes, so an unescaped expression
 * added later without that reasoning fails here rather than in a browser.
 */
class TemplateUnescapedOutputTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    private static final Pattern UTEXT = Pattern.compile("th:utext=\"([^\"]*)\"");
    private static final Pattern CONVERTER_OUTPUT = Pattern.compile("\\$\\{(\\w+\\.)?bodyHtml}");
    private static final String REASON = "<!-- th:utext:";

    @Test
    // trace:FR-001
    void unescaped_output_is_only_the_converters_and_says_why_in_a_comment_before_it() throws IOException {
        var found = 0;
        try (var files = Files.walk(TEMPLATES)) {
            for (var template :
                    files.filter(path -> path.toString().endsWith(".html")).toList()) {
                var text = Files.readString(template);
                var matcher = UTEXT.matcher(text);
                while (matcher.find()) {
                    found++;
                    assertThat(matcher.group(1))
                            .as("th:utext in %s shows the converter's output", template)
                            .matches(CONVERTER_OUTPUT);
                    assertThat(text.substring(0, matcher.start()))
                            .as("th:utext in %s has the comment giving its reason before it", template)
                            .contains(REASON);
                }
            }
        }

        // Without this the test passes on templates with no article body at all.
        assertThat(found).isPositive();
    }
}
