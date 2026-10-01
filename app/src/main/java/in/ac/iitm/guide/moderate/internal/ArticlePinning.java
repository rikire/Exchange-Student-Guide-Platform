package in.ac.iitm.guide.moderate.internal;

import in.ac.iitm.guide.moderate.persistence.ModerateArticleRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-025: the moderator pins, unpins and orders the landing page's pinned articles. The order is
 * {@code pin_position} (V8, ADR-0021): a newly pinned article goes last, and moving one swaps its place
 * with its neighbour's in the order, so a gap left by an unpinned or removed article never matters.
 */
// trace:FR-025
@Service
public class ArticlePinning {

    /** ADR-0010: the screen's list of published articles is bounded, as the public list is. */
    static final int PAGE_SIZE = 50;

    private final ModerateArticleRepository articles;

    ArticlePinning(ModerateArticleRepository articles) {
        this.articles = articles;
    }

    /** One line of the screen: an article, its tags, and whether it is pinned. */
    public record Line(String title, String address, String path, List<String> tags, boolean pinned) {}

    /**
     * @param pinned every pinned article, in order
     * @param published the page's published articles, by title
     */
    public record Screen(List<Line> pinned, List<Line> published, int page, int pages) {}

    /** @param page from 1 */
    @Transactional(readOnly = true)
    public Screen screen(int page) {
        var pinned = articles.findByPinnedAtIsNotNullAndRemovedAtIsNullOrderByPinPositionAsc().stream()
                .map(ArticlePinning::line)
                .toList();
        var published = articles.findByRemovedAtIsNull(PageRequest.of(
                page - 1, PAGE_SIZE, Sort.by(Sort.Order.asc("title").ignoreCase())));
        return new Screen(
                pinned, published.map(ArticlePinning::line).getContent(), page, Math.max(1, published.getTotalPages()));
    }

    /** Pins the article last; one already pinned keeps its place. */
    @Transactional
    public void pin(String address) {
        var article = live(address);
        if (article.getPinnedAt() != null) {
            return;
        }
        var last = pinnedInOrder().stream()
                .map(Article::getPinPosition)
                .max(Integer::compare)
                .orElse(0);
        article.setPinnedAt(OffsetDateTime.now());
        article.setPinPosition(last + 1);
    }

    @Transactional
    public void unpin(String address) {
        var article = live(address);
        article.setPinnedAt(null);
        article.setPinPosition(null);
    }

    /** Swaps the article's place with the one before it; the first stays first. */
    @Transactional
    public void moveUp(String address) {
        move(address, -1);
    }

    /** Swaps the article's place with the one after it; the last stays last. */
    @Transactional
    public void moveDown(String address) {
        move(address, 1);
    }

    private void move(String address, int step) {
        var article = live(address);
        var pinned = pinnedInOrder();
        var at = pinned.indexOf(article);
        var to = at + step;
        if (at < 0 || to < 0 || to >= pinned.size()) {
            return;
        }
        var neighbour = pinned.get(to);
        var place = article.getPinPosition();
        article.setPinPosition(neighbour.getPinPosition());
        neighbour.setPinPosition(place);
    }

    private List<Article> pinnedInOrder() {
        return articles.findByPinnedAtIsNotNullAndRemovedAtIsNullOrderByPinPositionAsc();
    }

    private Article live(String address) {
        return ArticleAddress.slugOf(address)
                .flatMap(articles::findBySlugAndRemovedAtIsNull)
                .orElseThrow(() -> new ArticleNotLiveException(address));
    }

    private static Line line(Article article) {
        return new Line(
                article.getTitle(),
                article.getSlug(),
                ArticleAddress.pathOf(article.getSlug()),
                article.getTags().stream().map(Tag::getName).sorted().toList(),
                article.getPinnedAt() != null);
    }
}
