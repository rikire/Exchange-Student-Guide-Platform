package in.ac.iitm.guide.report.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code guide.report.*}: how many reports one client address may send (ADR-0019's way, decided by the
 * human on 2 Oct), so a flood cannot bury the inbox. The value defaults in {@code application.yml}.
 */
// trace:FR-021
@ConfigurationProperties("guide.report")
public record ReportSettings(Limit limit) {

    /** At most {@code requests} in each {@code per}. */
    public record Limit(int requests, Duration per) {}
}
