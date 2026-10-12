package in.ac.iitm.guide.backup;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.backup.internal.SeedRunner;
import in.ac.iitm.guide.media.MediaTestFiles;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starting the application with the {@code seed} profile fills the guide from the seed files. The
 * tests share the seeded database on purpose and clean it once, after the last of them: on H2 each
 * context has a database of its own, but under {@code -P postgres} every context shares one, and the
 * seed's articles would collide with the titles other classes publish. The seed's PDFs are a few MB,
 * so the document and volume limits are large enough for them, not the few KB the other tests use.
 */
@SpringBootTest(properties = {"guide.media.document-limit=20MB", "guide.media.volume-limit=100MB"})
@ActiveProfiles("seed")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeedRunnerTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SeedRunner runner;

    @AfterAll
    void clearTheSeed() throws IOException {
        jdbc.execute("DELETE FROM media_asset");
        MediaTestFiles.empty(MediaTestFiles.ROOT);
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:NFR-004
    void the_seed_profile_loads_at_least_twenty_articles_including_the_frro_one() {
        var count = jdbc.queryForObject("SELECT count(*) FROM article", Integer.class);

        assertThat(count).isGreaterThanOrEqualTo(20);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM article WHERE title = 'Registering with FRRO'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    // trace:NFR-004
    void no_seeded_article_publishes_the_authors_open_questions() {
        assertThat(jdbc.queryForList("SELECT title FROM article WHERE body LIKE '%## Needs checking%'", String.class))
                .isEmpty();
    }

    @Test
    // trace:FR-034
    void the_seed_gives_the_articles_view_counts_frro_most_of_all() {
        var mostViewed =
                jdbc.queryForObject("SELECT title FROM article ORDER BY view_count DESC LIMIT 1", String.class);

        assertThat(mostViewed).isEqualTo("Registering with FRRO");
    }

    @Test
    // trace:FR-025
    void the_seed_pins_frro_first() {
        var first = jdbc.queryForObject(
                "SELECT title FROM article WHERE pin_position = 1 AND pinned_at IS NOT NULL", String.class);

        assertThat(first).isEqualTo("Registering with FRRO");
    }

    @Test
    // trace:FR-033
    void the_seed_attaches_the_study_in_india_slides_and_the_evisa_sample_form_to_their_articles() {
        var attached = jdbc.queryForList(
                "SELECT a.title || ' / ' || m.original_name FROM media_asset m JOIN article a ON a.id = m.article_id"
                        + " ORDER BY a.title",
                String.class);

        assertThat(attached)
                .containsExactly(
                        "Applying for Your Student Visa / Indian e-Visa - Sample Application Form.pdf",
                        "Filling in Your SII Student Dashboard / Study in India - Student Dashboard Process.pdf");
    }

    @Test
    // trace:NFR-004
    void running_the_seed_again_attaches_nothing_twice() {
        var before = jdbc.queryForObject("SELECT count(*) FROM media_asset", Integer.class);

        runner.seed();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_asset", Integer.class))
                .isEqualTo(before);
    }

    @Test
    // trace:NFR-004
    void running_the_seed_again_adds_nothing() {
        var before = jdbc.queryForObject("SELECT count(*) FROM article", Integer.class);

        runner.seed();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM article", Integer.class))
                .isEqualTo(before);
    }
}
