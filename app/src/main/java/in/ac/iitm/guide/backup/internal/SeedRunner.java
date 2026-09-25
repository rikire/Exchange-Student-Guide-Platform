package in.ac.iitm.guide.backup.internal;

import in.ac.iitm.guide.backup.ArticleArchive;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads the starter articles under {@code data/seed/} when the application starts with the
 * {@code seed} profile. The seed is on the classpath, so inside a jar it is not a directory and goes
 * through {@link ArticleArchive#importFiles} rather than {@code importFrom}. Running it again is
 * harmless: an article already there is skipped.
 */
// trace:NFR-004
@Component
@Profile("seed")
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final ArticleArchive archive;

    SeedRunner(ArticleArchive archive) {
        this.archive = archive;
    }

    @Override
    public void run(ApplicationArguments args) {
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
