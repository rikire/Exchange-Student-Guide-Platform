package in.ac.iitm.guide.about;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
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
    private static final Pattern DEVELOPER_CARD =
            Pattern.compile("<article class=\"dev-card\">.*?</article>", Pattern.DOTALL);

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

    @Test
    // trace:FR-035
    void every_developer_card_has_a_photo_and_a_link_to_each_account_the_developer_has() throws Exception {
        var cards = developerCards();

        assertThat(cards).hasSize(3);
        assertThat(cards).allMatch(card -> card.contains("<img class=\"person-photo\""), "a photo");
        assertThat(cards.get(0)).contains("href=\"https://t.me/Rizire\"", "href=\"https://github.com/rikire\"");
        assertThat(cards.get(1))
                .contains(
                        "href=\"https://t.me/abdra04\"",
                        "href=\"https://github.com/abdra04-gif\"",
                        "href=\"https://www.linkedin.com/in/abdirakhim-ismailov-55a8b1425\"");
        assertThat(cards.get(2)).contains("href=\"https://t.me/Yura_Rrrr\"", "href=\"https://github.com/Ten-Do\"");
    }

    @Test
    // trace:FR-035
    void no_developer_card_shows_a_blank() throws Exception {
        // The human, 12 Oct: a fact a developer does not have is left out of the card, not shown as ___.
        assertThat(developerCards()).noneMatch(card -> card.contains("placeholder"));
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

    /** The developer cards in page order, each as its own markup. */
    private List<String> developerCards() throws Exception {
        return DEVELOPER_CARD
                .matcher(page("/developers"))
                .results()
                .map(MatchResult::group)
                .toList();
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
    }
}
