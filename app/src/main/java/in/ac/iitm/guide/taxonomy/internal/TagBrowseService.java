package in.ac.iitm.guide.taxonomy.internal;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.taxonomy.persistence.TagBrowseRepository;
import in.ac.iitm.guide.taxonomy.persistence.TagRepository;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-008: the published articles carrying a tag, found by the tag's address (FEAT-008). The address
 * is computed, not stored, so the tag names are read from the tag table and matched here.
 */
// trace:FR-008
@Service
@Transactional(readOnly = true)
public class TagBrowseService {

    // ADR-0010: a page is bounded. No paging yet (FEAT-008, out of scope).
    static final int LIMIT = 50;

    private final TagBrowseRepository articles;
    private final TagRepository tags;
    private final DisplayTime displayTime;

    TagBrowseService(TagBrowseRepository articles, TagRepository tags, DisplayTime displayTime) {
        this.articles = articles;
        this.tags = tags;
        this.displayTime = displayTime;
    }

    /** ADR-0010's bound on the tag index; more than the guide is expected to hold. */
    static final int INDEX_LIMIT = 500;

    /** Fix 2.1 (F-6): every tag a live article carries, by name, up to {@link #INDEX_LIMIT}. */
    public TagIndex index() {
        var found = articles.findTagsInUse(PageRequest.of(0, INDEX_LIMIT + 1));
        var entries = found.stream()
                .limit(INDEX_LIMIT)
                .map(tag -> new TagIndex.Entry(TagLink.of(tag.getName()), tag.getArticles()))
                .toList();
        return new TagIndex(entries, found.size() > INDEX_LIMIT);
    }

    /** FR-031: counted only once the page is known to answer, so a {@code 404} counts nothing. */
    // trace:FR-031
    @Transactional
    public void countVisit(TagPage page) {
        tags.countVisit(page.names());
    }

    /**
     * @param address as it arrived in the path; put through the address rule, so case and
     *     punctuation do not matter
     * @return empty when no published article carries a tag with that address
     */
    public Optional<TagPage> browse(String address) {
        var wanted = ArticleAddress.slugOf(address);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        var names = articles.findAllTagNames().stream()
                .filter(name -> TagLink.addressOf(name).equals(wanted))
                .sorted()
                .toList();
        if (names.isEmpty()) {
            return Optional.empty();
        }
        // A tag only a pending, rejected or removed article carries is in the table all the same.
        var total = articles.countPublishedCarrying(names);
        if (total == 0) {
            return Optional.empty();
        }
        var entries = articles.findPublishedCarrying(names, PageRequest.of(0, LIMIT)).stream()
                .map(this::entry)
                .toList();
        return Optional.of(new TagPage(names, total, entries));
    }

    private TagPage.Entry entry(Article article) {
        return new TagPage.Entry(
                article.getTitle(),
                article.getSummary(),
                ArticleAddress.pathOf(article.getSlug()),
                TagLink.of(article.getTags().stream().map(Tag::getName).toList()),
                article.getUpdatedAt(),
                displayTime.date(article.getUpdatedAt()));
    }
}
