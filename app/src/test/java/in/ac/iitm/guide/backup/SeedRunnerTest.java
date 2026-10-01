package in.ac.iitm.guide.backup;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.backup.internal.SeedRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starting the application with the {@code seed} profile fills the guide from the seed files. The
 * tests share the seeded database on purpose and do not clean it: this context is the only one with
 * the profile, and each context has an in-memory database of its own, so nothing leaks into other
 * tests.
 */
@SpringBootTest
@ActiveProfiles("seed")
class SeedRunnerTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SeedRunner runner;

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
    // trace:NFR-004
    void running_the_seed_again_adds_nothing() {
        var before = jdbc.queryForObject("SELECT count(*) FROM article", Integer.class);

        runner.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM article", Integer.class))
                .isEqualTo(before);
    }
}
