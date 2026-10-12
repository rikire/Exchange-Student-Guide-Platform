package in.ac.iitm.guide.articleview.web;

import in.ac.iitm.guide.articleview.persistence.ArticleListRepository;
import in.ac.iitm.guide.media.ArticleDocument;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaItem;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@code GET /articles}: FR-033's list of every published article, by title, recently updated or most
 * viewed, narrowed to one tag, {@value #PAGE_SIZE} to a page.
 */
// trace:FR-033
@Controller
class ArticleListController {

    /** ADR-0010: a public list is bounded; the same as a tag's page. */
    static final int PAGE_SIZE = 50;

    /** The orders the list offers, by the word in its address. */
    enum Order {
        TITLE("title", "A–Z", Sort.by(Sort.Order.asc("title").ignoreCase())),
        UPDATED("updated", "Recently updated", Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("title"))),
        VIEWS("views", "Most viewed", Sort.by(Sort.Order.desc("viewCount"), Sort.Order.asc("title")));

        final String word;
        final String label;
        final Sort sort;

        Order(String word, String label, Sort sort) {
            this.word = word;
            this.label = label;
            this.sort = sort;
        }

        static Order of(String word) {
            return Arrays.stream(values())
                    .filter(order -> order.word.equals(word))
                    .findFirst()
                    .orElseThrow(() -> new BadListRequestException());
        }
    }

    /** What a card shows; {@code updated} is the fallback text, {@code updatedAt} for the reader's zone. */
    record Card(
            String title, String summary, String path, List<TagLink> tags, OffsetDateTime updatedAt, String updated) {}

    /** A document under the list: the file, and the article it is attached to. */
    record DocumentCard(MediaItem file, String articleTitle, String articlePath) {}

    /** An order or a tag the reader can switch to, and whether it is the one shown. */
    record Choice(String label, String href, boolean current) {}

    private final ArticleListRepository articles;
    private final DisplayTime displayTime;
    private final MediaAssets media;

    ArticleListController(ArticleListRepository articles, DisplayTime displayTime, MediaAssets media) {
        this.articles = articles;
        this.displayTime = displayTime;
        this.media = media;
    }

    @GetMapping("/articles")
    @Transactional(readOnly = true)
    String list(
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        var order = Order.of(sort);
        if (page < 1) {
            throw new BadListRequestException();
        }
        var request = PageRequest.of(page - 1, PAGE_SIZE, order.sort);

        // A tag's address follows the article address rule, as on its own page (FEAT-008).
        var wanted = tag == null ? null : ArticleAddress.slugOf(tag).orElse("");
        var inUse = articles.findTagNamesInUse();
        var tagNames = wanted == null
                ? List.<String>of()
                : inUse.stream()
                        .filter(name ->
                                TagLink.addressOf(name).filter(wanted::equals).isPresent())
                        .toList();
        Page<Article> found;
        if (tag != null && tagNames.isEmpty()) {
            found = Page.empty(request);
        } else {
            found = tag == null
                    ? articles.findByRemovedAtIsNull(request)
                    : articles.findPublishedCarrying(tagNames, request);
        }
        if (page > 1 && page > found.getTotalPages()) {
            throw new ArticleNotFoundException("page " + page);
        }

        model.addAttribute("cards", found.getContent().stream().map(this::card).toList());
        model.addAttribute(
                "orders",
                Arrays.stream(Order.values())
                        .map(o -> new Choice(o.label, address(o, wanted, 1), o == order))
                        .toList());
        model.addAttribute(
                "tags",
                inUse.stream()
                        .flatMap(name -> TagLink.addressOf(name).stream()
                                .map(address -> new Choice(name, address(order, address, 1), address.equals(wanted))))
                        .distinct()
                        .toList());
        model.addAttribute("allTags", address(order, null, 1));
        model.addAttribute("tag", wanted);
        model.addAttribute("tagUnknown", tag != null && tagNames.isEmpty());
        model.addAttribute("sort", order.word);
        model.addAttribute("total", found.getTotalElements());
        model.addAttribute("page", page);
        model.addAttribute("pages", found.getTotalPages());
        model.addAttribute("previous", page > 1 ? address(order, wanted, page - 1) : null);
        model.addAttribute("next", found.hasNext() ? address(order, wanted, page + 1) : null);
        // Under the first page only, whatever the order or the tag: one place to find every document.
        model.addAttribute("documents", page == 1 ? documents() : List.of());
        return "articleview/ArticleList";
    }

    /** Two queries whatever the number of documents: theirs, then their articles' titles. */
    private List<DocumentCard> documents() {
        var attached = media.documentsOfPublishedArticles(PAGE_SIZE);
        if (attached.isEmpty()) {
            return List.of();
        }
        var owners = articles
                .findByIdInAndRemovedAtIsNull(
                        attached.stream().map(ArticleDocument::articleId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Article::getId, article -> article));
        return attached.stream()
                .filter(document -> owners.containsKey(document.articleId()))
                .map(document -> {
                    var article = owners.get(document.articleId());
                    return new DocumentCard(
                            document.document(), article.getTitle(), ArticleAddress.pathOf(article.getSlug()));
                })
                .toList();
    }

    private Card card(Article article) {
        return new Card(
                article.getTitle(),
                article.getSummary(),
                ArticleAddress.pathOf(article.getSlug()),
                TagLink.of(article.getTags().stream().map(Tag::getName).toList()),
                article.getUpdatedAt(),
                displayTime.date(article.getUpdatedAt()));
    }

    /** The order and the tag are part of the address, so a list can be linked to (FR-033). */
    private static String address(Order order, String tag, int page) {
        var query = new StringBuilder();
        if (order != Order.TITLE) {
            query.append("&sort=").append(order.word);
        }
        if (tag != null) {
            query.append("&tag=").append(tag);
        }
        if (page > 1) {
            query.append("&page=").append(page);
        }
        return query.isEmpty() ? "/articles" : "/articles?" + query.substring(1);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadListRequestException extends RuntimeException {}
}
