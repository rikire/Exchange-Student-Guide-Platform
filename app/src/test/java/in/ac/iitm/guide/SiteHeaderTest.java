package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Walkthrough fix 2.1 (F-6): the header of every public page leads to all articles, the tags, the
 * tracking page and the form, so a contributor who closed the confirmation can still find their
 * submission.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SiteHeaderTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/articles", "/tags", "/search?q=frro", "/submit", "/submissions/status"})
    // trace:FR-009
    void the_header_links_to_articles_tags_tracking_and_the_form(String path) throws Exception {
        var page = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
        var header = page.substring(page.indexOf("<header"), page.indexOf("</header>"));

        assertThat(header)
                .contains("href=\"/articles\"")
                .contains("href=\"/tags\"")
                .contains("href=\"/submissions/status\"")
                .contains("href=\"/submit\"");
    }

    @org.junit.jupiter.api.Test
    // trace:FR-014
    void the_moderator_login_carries_the_moderator_label() throws Exception {
        var header = header("/moderate/login");

        // Walkthrough-fixes 3.9, N9: the label of docs/design/screens/AdminLogin.html.
        assertThat(header).contains("<span class=\"role-label\">Moderator</span>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/articles", "/submit"})
    // trace:FR-009
    void a_readers_header_carries_no_moderator_label(String path) throws Exception {
        var header = header(path);

        assertThat(header).doesNotContain("role-label");
    }

    private String header(String path) throws Exception {
        var page = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
        return page.substring(page.indexOf("<header"), page.indexOf("</header>"));
    }
}
