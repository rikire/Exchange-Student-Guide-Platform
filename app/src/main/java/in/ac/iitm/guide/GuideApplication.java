package in.ac.iitm.guide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Exchange Student Guide.
 *
 * <p>This class also marks the root of the Spring Modulith application module structure: every
 * package directly below this one is a slice, and its nested packages are internal to that slice.
 * See docs/ai/architecture-rules.md.
 */
@SpringBootApplication
public class GuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideApplication.class, args);
    }
}
