package in.ac.iitm.guide.contribute.web;

import java.time.Duration;

/**
 * The wait NFR-005 imposes, as a {@code Retry-After} header counts it and as a person reads it: whole
 * seconds and whole minutes, each rounded up, so neither tells a client to come back too early.
 */
record RetryAfter(Duration delay) {

    long seconds() {
        return Math.max(1, (delay.toNanos() + 999_999_999) / 1_000_000_000);
    }

    long minutes() {
        return (seconds() + 59) / 60;
    }
}
