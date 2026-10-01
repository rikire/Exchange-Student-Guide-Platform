package in.ac.iitm.guide.backup.internal;

import in.ac.iitm.guide.backup.ArticleArchive;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads the starter articles under {@code data/seed/} when the application starts with the
 * {@code seed} profile. The seed is on the classpath, so inside a jar it is not a directory and goes
 * through {@link ArticleArchive#importFiles} rather than {@code importFrom}. Running it again is
 * harmless: an article already there is skipped.
 *
 * <p>It runs as the context starts, before the web server does, so the guide answers its first
 * request with the whole seed in place (walkthrough fix 1.6). As an {@code ApplicationRunner} it ran
 * after the server, and a search in the first second after a start found nothing.
 */
// trace:NFR-004
@Component
@Profile("seed")
public class SeedRunner implements SmartLifecycle {

    /**
     * Before search's index builder, which has to see what the seed wrote, and before the web server,
     * which Spring Boot starts at {@code SMART_LIFECYCLE_PHASE - 1024}.
     */
    public static final int PHASE = WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 4096;

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final ArticleArchive archive;
    private volatile boolean running;

    SeedRunner(ArticleArchive archive) {
        this.archive = archive;
    }

    @Override
    public void start() {
        seed();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    public void seed() {
        var files = new TreeMap<String, String>();
        try {
            for (var resource : new PathMatchingResourcePatternResolver().getResources("classpath:data/seed/*.md")) {
                // The README describes the directory; it is the one file there that is not an article.
                if (!"README.md".equals(resource.getFilename())) {
                    files.put(resource.getFilename(), resource.getContentAsString(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the seed articles", e);
        }
        var report = archive.importFiles(files);
        log.info(
                "Seed: {} articles imported, {} already there",
                report.imported(),
                report.skipped().size());
    }
}
