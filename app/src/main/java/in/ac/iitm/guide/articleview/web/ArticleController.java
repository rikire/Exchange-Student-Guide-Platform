package in.ac.iitm.guide.articleview.web;

import in.ac.iitm.guide.articleview.persistence.ArticleReadRepository;
import in.ac.iitm.guide.articleview.persistence.ArticleViewRepository;
import in.ac.iitm.guide.backlink.Backlinks;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaItem;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** {@code GET /articles/{address}}: one published article, its body rendered, wiki links resolved. */
// trace:FR-001
// trace:FR-002
// trace:FR-004
// trace:FR-030
// trace:FR-026
// trace:FR-006
// trace:FR-034
@Controller
class ArticleController {

    /** The role the moderator login grants (ADR-0009); only it is offered FR-026's removal. */
    private static final String MODERATOR = "MODERATOR";

    /** Fix 3.5 (F-10): a contents list once an article has this many headings; fewer need none. */
    static final int CONTENTS_FROM = 3;

    private final ArticleReadRepository articles;
    private final ArticleViewRepository views;
    private final MediaAssets media;
    private final Backlinks backlinks;
    private final DisplayTime displayTime;

    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    ArticleController(
            ArticleReadRepository articles,
            ArticleViewRepository views,
            MediaAssets media,
            Backlinks backlinks,
            DisplayTime displayTime) {
        this.articles = articles;
        this.views = views;
        this.media = media;
        this.backlinks = backlinks;
        this.displayTime = displayTime;
    }

    @GetMapping("/articles/{address}")
    String show(@PathVariable String address, Model model, HttpServletRequest request) {
        // The address is put through the same rule as a title, so /articles/HOSTEL-Life reaches the
        // article stored under "hostel-life" (ui-routes.md: matched case-insensitively).
        var slug = ArticleAddress.slugOf(address).orElseThrow(() -> new ArticleNotFoundException(address));
        var article =
                articles.findBySlugAndRemovedAtIsNull(slug).orElseThrow(() -> new ArticleNotFoundException(address));

        var tags = TagLink.of(article.getTags().stream().map(Tag::getName).toList());
        var body = renderer.renderBody(article.getBody(), this::resolve);
        model.addAttribute(
                "article",
                new ArticlePage(
                        article.getTitle(),
                        article.getSummary(),
                        article.getUpdatedAt(),
                        displayTime.date(article.getUpdatedAt()),
                        tags,
                        body.html(),
                        body.contents().size() >= CONTENTS_FROM ? body.contents() : List.of(),
                        media.ofArticle(article.getId()),
                        ArticleAddress.pathOf(article.getSlug()) + "/edit",
                        request.isUserInRole(MODERATOR)
                                ? "/moderate/articles/" + article.getSlug() + "/remove"
                                : null));
        model.addAttribute("backlinks", backlinks.linkingTo(article.getId()));
        // FR-034: the office checking its own guide is not a reader.
        if (!request.isUserInRole(MODERATOR)) {
            views.countView(article.getId());
        }
        return "articleview/Article";
    }

    /** Which of these titles is a live article, and where: one query for the whole page. */
    private Map<String, String> resolve(Set<String> titles) {
        var slugOfTitle = new HashMap<String, String>();
        for (var title : titles) {
            ArticleAddress.slugOf(title).ifPresent(slug -> slugOfTitle.put(title, slug));
        }
        if (slugOfTitle.isEmpty()) {
            return Map.of();
        }

        var live = articles.findLiveSlugs(new HashSet<>(slugOfTitle.values()));

        var hrefs = new HashMap<String, String>();
        slugOfTitle.forEach((title, slug) -> {
            if (live.contains(slug)) {
                hrefs.put(title, ArticleAddress.pathOf(slug));
            }
        });
        return hrefs;
    }

    /**
     * What the template shows; {@code bodyHtml} is the converter's output and is the only unescaped
     * part. {@code updated} is {@code updatedAt} as text, the fallback without scripts (ADR-0020);
     * {@code contents} is empty below {@link #CONTENTS_FROM} headings.
     * {@code media} are the attached assets (FR-001's Article, FEAT-009). {@code editPath} leads
     * to proposing an edit (FR-011, {@code contribute}). {@code removePath} leads to FR-026's removal
     * and is {@code null} for anyone but the signed-in moderator.
     */
    record ArticlePage(
            String title,
            String summary,
            OffsetDateTime updatedAt,
            String updated,
            List<TagLink> tags,
            String bodyHtml,
            List<WikiLinkRenderer.Heading> contents,
            List<MediaItem> media,
            String editPath,
            String removePath) {}
}
