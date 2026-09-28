package in.ac.iitm.guide.backup;

import in.ac.iitm.guide.backlink.ArticleTextChanged;
import in.ac.iitm.guide.backup.internal.ArchivedArticle;
import in.ac.iitm.guide.backup.internal.FrontMatter;
import in.ac.iitm.guide.backup.persistence.ArchiveArticleRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.TagRejectedException;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the knowledge base from a set of Markdown files with YAML front matter (ADR-0007) and
 * writes it back out in the same format.
 */
// trace:NFR-004
// trace:FR-006
@Service
public class ArticleArchive {

    /** The one file in a seed or export directory that describes the directory, not an article. */
    private static final String README = "README.md";

    private static final int EXPORT_PAGE_SIZE = 100;

    private final ArchiveArticleRepository articles;
    private final Tags tags;
    private final ApplicationEventPublisher events;

    ArticleArchive(ArchiveArticleRepository articles, Tags tags, ApplicationEventPublisher events) {
        this.articles = articles;
        this.tags = tags;
        this.events = events;
    }

    /**
     * Creates one published article per file; {@code files} maps a file name to its text. A file
     * whose title already belongs to an article is skipped and reported. One bad file rolls the whole
     * import back: a half-loaded knowledge base is worse than none.
     */
    @Transactional
    public ImportReport importFiles(Map<String, String> files) {
        var imported = 0;
        var skipped = new ArrayList<String>();
        // Sorted, so the same input always fails on the same file.
        for (var file : new TreeMap<>(files).entrySet()) {
            var parsed = FrontMatter.parse(file.getKey(), file.getValue());
            var slug = ArticleAddress.slugOf(parsed.title()).orElseThrow();
            var existing = articles.findBySlug(slug);
            if (existing.isPresent()) {
                if (!existing.get().getTitle().equals(parsed.title())) {
                    throw new ArchiveFormatException(file.getKey() + ": the title \"" + parsed.title()
                            + "\" has the same address as the existing article \""
                            + existing.get().getTitle() + "\"");
                }
                skipped.add(parsed.title());
                continue;
            }
            var article = new Article();
            article.setTitle(parsed.title());
            article.setSlug(slug);
            article.setSummary(parsed.summary());
            article.setBody(parsed.body());
            article.setPublishedAt(parsed.created());
            article.setUpdatedAt(parsed.updated());
            article.setPinnedAt(parsed.pinned());
            article.setTags(tagsNamed(file.getKey(), parsed.tags()));
            articles.save(article);
            events.publishEvent(new ArticleTextChanged(article.getId()));
            imported++;
        }
        return new ImportReport(imported, List.copyOf(skipped));
    }

    /**
     * Creates the articles of every {@code .md} file directly in {@code directory}, except
     * {@code README.md}; other files and subdirectories are not read. Transactional in its own
     * right: {@link #importFiles} is called from inside this class, not through the proxy.
     */
    @Transactional
    public ImportReport importFrom(Path directory) {
        var files = new TreeMap<String, String>();
        try (var entries = Files.list(directory)) {
            for (var path : entries.toList()) {
                var name = path.getFileName().toString();
                if (Files.isRegularFile(path) && name.endsWith(".md") && !name.equals(README)) {
                    files.put(name, Files.readString(path, StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the archive directory " + directory, e);
        }
        return importFiles(files);
    }

    /**
     * Writes every published article to {@code directory} as {@code <address>.md}, creating the
     * directory if needed, and returns how many files it wrote. An existing file is never
     * overwritten, so the directory should be empty: a failure part-way leaves the files already
     * written.
     */
    @Transactional(readOnly = true)
    public int exportTo(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create the export directory " + directory, e);
        }
        var written = 0;
        Pageable pageable = PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("slug"));
        Slice<Article> page;
        do {
            page = articles.findByRemovedAtIsNull(pageable);
            for (var article : page) {
                write(directory.resolve(article.getSlug() + ".md"), archived(article));
                written++;
            }
            pageable = page.nextPageable();
        } while (page.hasNext());
        return written;
    }

    private static ArchivedArticle archived(Article article) {
        var tagNames = article.getTags().stream().map(Tag::getName).sorted().toList();
        return new ArchivedArticle(
                article.getTitle(),
                article.getSummary(),
                tagNames,
                article.getPublishedAt(),
                article.getUpdatedAt(),
                article.getPinnedAt(),
                article.getBody());
    }

    private static void write(Path file, ArchivedArticle article) {
        try {
            Files.writeString(file, FrontMatter.render(article), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }

    /** Through {@code taxonomy}, which owns the tag rule (ADR-0005); a rejected tag names its file. */
    private Set<Tag> tagsNamed(String fileName, List<String> names) {
        try {
            return tags.named(names);
        } catch (TagRejectedException e) {
            throw new ArchiveFormatException(fileName + ": " + e.getMessage());
        }
    }
}
