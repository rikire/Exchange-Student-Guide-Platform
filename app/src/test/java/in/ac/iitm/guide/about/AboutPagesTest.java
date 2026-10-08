package in.ac.iitm.guide.about;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** FR-035: the OGE team and developers pages, static and open to anyone (FEAT-022). */
@SpringBootTest
@AutoConfigureMockMvc
class AboutPagesTest {

    private static final Pattern IMAGE_SOURCE = Pattern.compile("<img[^>]*\\ssrc=\"([^\"]*)\"");

    @Autowired
    private MockMvc mockMvc;

    @Test
    // trace:FR-035
    void the_oge_team_page_answers_200_to_anyone() throws Exception {
        mockMvc.perform(get("/oge-team")).andExpect(status().isOk());
    }

    @Test
    // trace:FR-035
    void the_developers_page_answers_200_to_anyone() throws Exception {
        mockMvc.perform(get("/developers")).andExpect(status().isOk());
    }

    @Test
    // trace:FR-035
    void the_oge_team_page_puts_the_dean_before_the_inbound_contacts() throws Exception {
        var page = page("/oge-team");

        assertThat(page).contains("Preeti Aghalayam", "Thukaram M Damodhar", "Karlin Keziah");
        assertThat(page.indexOf("Preeti Aghalayam"))
                .as("the Dean comes first")
                .isLessThan(page.indexOf("Thukaram M Damodhar"))
                .isLessThan(page.indexOf("Karlin Keziah"));
    }

    @Test
    // trace:FR-035
    void the_developers_page_names_all_three_developers() throws Exception {
        assertThat(page("/developers")).contains("Mikhail Novikov", "Abdirakhim Ismailov", "Iurii Rudenko");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/oge-team", "/developers"})
    // trace:FR-035
    void every_image_on_the_about_pages_is_served_from_the_site(String path) throws Exception {
        var page = page(path);
        var sources = new ArrayList<String>();
        IMAGE_SOURCE.matcher(page).results().forEach(match -> sources.add(match.group(1)));

        // ADR-0013's policy allows images from the site only; one from elsewhere would show broken.
        assertThat(sources).isNotEmpty().allMatch(source -> source.startsWith("/"), "a path on this site");
        for (var source : sources) {
            mockMvc.perform(get(source)).andExpect(status().isOk());
        }
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
    }
}
