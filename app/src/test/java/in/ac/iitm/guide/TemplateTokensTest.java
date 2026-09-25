package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Phase 2 step 3's check: a template carries class names and token names, never a literal colour or
 * spacing value. The values live in one place ({@code static/css/tokens.css}, taken from
 * docs/design/reference.md), so a change to the palette is one edit, not a hunt through pages.
 */
class TemplateTokensTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    private static final Pattern HEX_COLOUR = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
    private static final Pattern CSS_LENGTH = Pattern.compile("\\b\\d+(\\.\\d+)?(px|rem|em)\\b");
    private static final Pattern INLINE_STYLE = Pattern.compile("\\sstyle\\s*=");

    @Test
    void a_template_holds_no_literal_colour_length_or_inline_style() throws IOException {
        try (var files = Files.walk(TEMPLATES)) {
            var templates =
                    files.filter(path -> path.toString().endsWith(".html")).collect(Collectors.toList());

            // Without this the test passes on an application with no templates at all.
            assertThat(templates).isNotEmpty();

            for (var template : templates) {
                var text = Files.readString(template);
                assertThat(HEX_COLOUR.matcher(text).find())
                        .as("literal colour in %s", template)
                        .isFalse();
                assertThat(CSS_LENGTH.matcher(text).find())
                        .as("literal length in %s", template)
                        .isFalse();
                assertThat(INLINE_STYLE.matcher(text).find())
                        .as("inline style in %s", template)
                        .isFalse();
            }
        }
    }
}
