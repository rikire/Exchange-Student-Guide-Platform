package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.boot.test.context.SpringBootTest;

/** Fails when the application context cannot be built at all, and holds what it is built with. */
@SpringBootTest
class ApplicationSmokeTest {

    @Autowired
    private ThymeleafProperties thymeleaf;

    @Test
    void context_loads() {}

    @Test
    // DEBT-020: the stand runs without a profile for templates, so caching is the default; only the
    // dev profile turns it off to show an edited template on reload.
    void templates_are_cached_unless_the_dev_profile_says_otherwise() {
        assertThat(thymeleaf.isCache()).isTrue();
    }
}
