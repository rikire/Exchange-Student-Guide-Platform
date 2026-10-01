package in.ac.iitm.guide.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Plain Java: the zone comes from a setting, so the class is tried at the default and at another. */
class DisplayTimeTest {

    private static final OffsetDateTime SENT = OffsetDateTime.parse("2026-10-01T11:58:00Z");

    @Test
    // trace:FR-014
    void a_moment_is_shown_in_india_time_with_its_label() {
        var display = new DisplayTime("Asia/Kolkata");

        assertThat(display.dateTime(SENT)).isEqualTo("1 Oct 2026, 17:28 IST");
    }

    @Test
    // trace:FR-014
    void another_zone_in_the_setting_changes_the_time_and_the_label() {
        var display = new DisplayTime("UTC");

        assertThat(display.dateTime(SENT)).isEqualTo("1 Oct 2026, 11:58 UTC");
    }

    @Test
    // trace:FR-008
    void the_date_is_the_day_in_the_zone_not_the_stored_one() {
        var display = new DisplayTime("Asia/Kolkata");

        assertThat(display.date(OffsetDateTime.parse("2026-09-30T20:00:00Z"))).isEqualTo("1 Oct 2026");
    }

    @Test
    // trace:FR-014
    void an_unknown_zone_is_refused_at_once() {
        assertThatThrownBy(() -> new DisplayTime("Asia/Nowhere")).isInstanceOf(DateTimeException.class);
    }
}
