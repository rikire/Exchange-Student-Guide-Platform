package in.ac.iitm.guide.media.internal;

import in.ac.iitm.guide.media.MediaKind;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * {@code guide.media.*}: where the bytes live (ADR-0006), NFR-001's limits, and how long a rejected
 * submission's files are kept (DEBT-016), which are settings rather than constants. Their values are
 * in {@code application.yml}, the one place they default.
 */
// trace:NFR-001
@ConfigurationProperties("guide.media")
public record MediaSettings(
        Path root,
        DataSize photoLimit,
        DataSize documentLimit,
        DataSize videoLimit,
        DataSize volumeLimit,
        Duration rejectedKeptFor) {

    public DataSize limitOf(MediaKind kind) {
        return switch (kind) {
            case PHOTO -> photoLimit;
            case DOCUMENT -> documentLimit;
            case VIDEO -> videoLimit;
        };
    }

    /** @return the size as a contributor reads it: {@code 10 MB}, {@code 64 KB} */
    public static String spoken(DataSize size) {
        var bytes = size.toBytes();
        if (bytes % DataSize.ofGigabytes(1).toBytes() == 0) {
            return size.toGigabytes() + " GB";
        }
        if (bytes % DataSize.ofMegabytes(1).toBytes() == 0) {
            return size.toMegabytes() + " MB";
        }
        if (bytes % DataSize.ofKilobytes(1).toBytes() == 0) {
            return size.toKilobytes() + " KB";
        }
        return bytes + " bytes";
    }
}
