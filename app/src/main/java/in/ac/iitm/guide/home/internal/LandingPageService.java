package in.ac.iitm.guide.home.internal;

import in.ac.iitm.guide.home.persistence.LandingReadRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the landing page. Transactional so the tags of the listed articles load inside the
 * session, in batches (spring.jpa.properties.hibernate.default_batch_fetch_size), and the view
 * never reaches back into the database.
 */
// trace:FR-009
@Service
@Transactional(readOnly = true)
public class LandingPageService {

    // The requirement fixes neither number. Both bound the page (ADR-0010) and are here, in one
    // place, so changing one is one edit.
    static final int PINNED_LIMIT = 12;
    static final int RECENT_LIMIT = 12;
    static final int TAG_LIMIT = 50;

    private final LandingReadRepository articles;
    private final DisplayTime displayTime;

    LandingPageService(LandingReadRepository articles, DisplayTime displayTime) {
        this.articles = articles;
        this.displayTime = displayTime;
    }

    public LandingPage build() {
        var pinned = articles.findByPinnedAtIsNotNullAndRemovedAtIsNullOrderByPinPositionAsc(
                PageRequest.of(0, PINNED_LIMIT));
        var recent =
                articles.findByPinnedAtIsNullAndRemovedAtIsNullOrderByPublishedAtDesc(PageRequest.of(0, RECENT_LIMIT));
        var tags = articles.findTagsInUse(PageRequest.of(0, TAG_LIMIT)).stream()
                .map(tag -> new LandingPage.ListedTag(
                        tag.getName(), TagLink.of(tag.getName()).path(), tag.getArticles()))
                .toList();
        return new LandingPage(cards(pinned), cards(recent), tags);
    }

    private List<LandingPage.Card> cards(List<Article> articles) {
        return articles.stream().map(this::card).toList();
    }

    private LandingPage.Card card(Article article) {
        // A title with no letters or digits has no address and could not have been published.
        var path = ArticleAddress.pathOf(article.getSlug());
        var tags = TagLink.of(article.getTags().stream().map(Tag::getName).toList());
        return new LandingPage.Card(
                article.getTitle(),
                article.getSummary(),
                path,
                tags,
                article.getUpdatedAt(),
                displayTime.date(article.getUpdatedAt()));
    }
}
