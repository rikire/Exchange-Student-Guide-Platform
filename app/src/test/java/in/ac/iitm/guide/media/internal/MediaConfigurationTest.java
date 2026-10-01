package in.ac.iitm.guide.media.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * The container's multipart ceiling follows NFR-001's limits, so raising one in {@code guide.media}
 * is enough: a ceiling set apart from them would refuse the larger file before media saw it.
 */
class MediaConfigurationTest {

    private static final DataSize TEXT_ROOM = DataSize.ofMegabytes(10);

    @Test
    // trace:NFR-001
    void the_file_ceiling_is_the_largest_limit_and_the_request_has_room_for_the_text() {
        var multipart = new MediaConfiguration().multipartConfig(settings(10, 20, 500));

        assertThat(multipart.getMaxFileSize())
                .isEqualTo(DataSize.ofMegabytes(500).toBytes());
        assertThat(multipart.getMaxRequestSize())
                .isEqualTo(DataSize.ofMegabytes(500).toBytes() + TEXT_ROOM.toBytes());
    }

    @Test
    // trace:NFR-001
    void the_ceiling_follows_whichever_kind_is_largest_not_always_the_video() {
        var multipart = new MediaConfiguration().multipartConfig(settings(10, 700, 500));

        assertThat(multipart.getMaxFileSize())
                .isEqualTo(DataSize.ofMegabytes(700).toBytes());
    }

    private static MediaSettings settings(long photoMb, long documentMb, long videoMb) {
        return new MediaSettings(
                Path.of("unused"),
                DataSize.ofMegabytes(photoMb),
                DataSize.ofMegabytes(documentMb),
                DataSize.ofMegabytes(videoMb),
                DataSize.ofGigabytes(100));
    }
}
