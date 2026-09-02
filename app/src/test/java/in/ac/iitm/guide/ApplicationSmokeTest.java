package in.ac.iitm.guide;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Fails when the application context cannot be built at all. */
@SpringBootTest
class ApplicationSmokeTest {

    @Test
    void context_loads() {}
}
