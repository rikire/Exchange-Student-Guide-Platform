package in.ac.iitm.guide.contribute.internal;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Issues submission numbers and reads one back as a contributor typed it (ADR-0011, NFR-006): 60
 * random bits as 12 Crockford base32 characters, shown as {@code SUB-K7M2-QX9P-4TVB}. The prefix and
 * the hyphens are presentation; holding the 60 bits is the only authorisation there is.
 *
 * <p>Written here rather than taken from a library: Guava's and Commons Codec's base32 are RFC 4648,
 * whose alphabet keeps {@code I}, {@code L}, {@code O} and {@code U}, the characters Crockford drops
 * because people mistype them, and 60 bits is exactly twelve 5-bit groups, so no padding rule is
 * needed.
 */
// trace:NFR-006
@Component
public class SubmissionNumbers {

    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int CHARACTERS = 12;
    private static final long SIXTY_BITS = (1L << 60) - 1;
    private static final String PREFIX = "SUB";
    private static final Pattern TYPED = Pattern.compile("(?:SUB)?([0-9A-HJKMNP-TV-Z]{12})");

    private final RandomGenerator source;

    public SubmissionNumbers() {
        this(new SecureRandom());
    }

    SubmissionNumbers(RandomGenerator source) {
        this.source = source;
    }

    RandomGenerator source() {
        return source;
    }

    /** @return a new number, {@code SUB-} and three groups of four Crockford base32 characters */
    public String next() {
        var bits = source.nextLong() & SIXTY_BITS;
        var characters = new char[CHARACTERS];
        for (var i = CHARACTERS - 1; i >= 0; i--) {
            characters[i] = CROCKFORD[(int) (bits & 31)];
            bits >>>= 5;
        }
        return format(new String(characters));
    }

    /**
     * @param typed a number as a contributor typed it: any case, with or without the hyphens and the
     *     prefix
     * @return the form it is stored in, or empty when the text cannot be a number
     */
    public static Optional<String> canonical(String typed) {
        var compact = typed.strip().replace("-", "").toUpperCase(Locale.ROOT);
        var matcher = TYPED.matcher(compact);
        return matcher.matches() ? Optional.of(format(matcher.group(1))) : Optional.empty();
    }

    private static String format(String characters) {
        return PREFIX + "-" + characters.substring(0, 4) + "-" + characters.substring(4, 8) + "-"
                + characters.substring(8);
    }
}
