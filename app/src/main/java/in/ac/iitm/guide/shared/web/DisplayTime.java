package in.ac.iitm.guide.shared.web;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * How every page shows a moment: in the office's time zone, {@code guide.time-zone}, and in English
 * whatever the browser asks for. Times are stored as they happened; only their display is converted.
 */
@Component
public class DisplayTime {

    // English whatever the browser asks for (the human, 28 Sep): the guide is written in English.
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm z", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final ZoneId zone;

    /** @param timeZone a region id such as {@code Asia/Kolkata}; an unknown one stops the start-up */
    public DisplayTime(@Value("${guide.time-zone}") String timeZone) {
        this.zone = ZoneId.of(timeZone);
    }

    /** @return the date and the time, with the zone's abbreviation: {@code 1 Oct 2026, 17:28 IST} */
    public String dateTime(OffsetDateTime moment) {
        return DATE_TIME.format(moment.atZoneSameInstant(zone));
    }

    /** @return the date in the zone, which may be a day after the stored one */
    public String date(OffsetDateTime moment) {
        return DATE.format(moment.atZoneSameInstant(zone));
    }
}
