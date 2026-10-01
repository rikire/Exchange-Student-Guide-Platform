package in.ac.iitm.guide.media.internal;

import in.ac.iitm.guide.media.MediaKind;
import jakarta.servlet.MultipartConfigElement;
import java.util.Arrays;
import java.util.Comparator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.unit.DataSize;

@Configuration
@EnableConfigurationProperties(MediaSettings.class)
// RejectedMediaSweep is the application's only scheduled work so far.
@EnableScheduling
class MediaConfiguration {

    /** What a form carries beside its file: the title, the summary, a body of up to 100,000 characters. */
    private static final DataSize TEXT_ROOM = DataSize.ofMegabytes(10);

    /**
     * The container's ceiling, above which a form is refused unread (413, FEAT-009): the largest of
     * NFR-001's limits for the file, and room for the text beside it. Derived rather than set, so
     * that raising a limit in {@code guide.media} is enough; this bean takes the place of Spring
     * Boot's {@code spring.servlet.multipart}, which backs off when one is defined.
     */
    // TODO(DEBT-015): more than 2 MB over this, Tomcat closes the connection instead of answering.
    @Bean
    MultipartConfigElement multipartConfig(MediaSettings media) {
        var largest = Arrays.stream(MediaKind.values())
                .map(media::limitOf)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        var factory = new MultipartConfigFactory();
        factory.setMaxFileSize(largest);
        factory.setMaxRequestSize(DataSize.ofBytes(largest.toBytes() + TEXT_ROOM.toBytes()));
        return factory.createMultipartConfig();
    }
}
