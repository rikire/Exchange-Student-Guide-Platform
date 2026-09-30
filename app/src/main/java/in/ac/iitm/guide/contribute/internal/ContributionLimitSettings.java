package in.ac.iitm.guide.contribute.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code guide.contribute.*}: NFR-005's limits on what one client address may send, which are settings
 * rather than constants. Their values are in {@code application.yml}, the one place they default.
 */
// trace:NFR-005
@ConfigurationProperties("guide.contribute")
public record ContributionLimitSettings(Limit submissionLimit, Limit previewLimit) {

    /** At most {@code requests} in each {@code per}. */
    public record Limit(int requests, Duration per) {}
}
