package in.ac.iitm.guide.articleview.web;

import in.ac.iitm.guide.articleview.persistence.ArticleReadRepository;
import in.ac.iitm.guide.articleview.persistence.ArticleViewRepository;
import in.ac.iitm.guide.backlink.Backlinks;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaItem;
import in.ac.iitm.guide.search.SimilarTitles;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

/** {@code GET /articles/{address}}: one published article, its body rendered, wiki links resolved. */
// trace:FR-001
// trace:FR-005
// trace:FR-021
// trace:FR-024
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
    private final SimilarTitles similarTitles;

    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    ArticleController(
            ArticleReadRepository articles,
            ArticleViewRepository views,
            MediaAssets media,
            Backlinks backlinks,
            DisplayTime displayTime,
            SimilarTitles similarTitles) {
        this.articles = articles;
        this.views = views;
        this.media = media;
        this.backlinks = backlinks;
        this.displayTime = displayTime;
        this.similarTitles = similarTitles;
    }

    @GetMapping("/articles/{address}")
    String show(
            @PathVariable String address,
            @RequestParam(name = "title", required = false) String invitedTitle,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        // The address is put through the same rule as a title, so /articles/HOSTEL-Life reaches the
        // article stored under "hostel-life" (ui-routes.md: matched case-insensitively).
        var found = ArticleAddress.slugOf(address).flatMap(articles::findBySlugAndRemovedAtIsNull);
        if (found.isEmpty()) {
            // Fix 3.8 (F-14): the not-found page itself, rendered here rather than by the error
            // dispatch, so it can offer titles close to the address.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("path", request.getRequestURI());
            model.addAttribute("suggestions", similarTitles.near(address));
            // FR-005: a red link's title, offered to the submission form, only when it is the title
            // this address belongs to, so a crafted link cannot put other words in the invitation.
            if (invitedTitle != null && ArticleAddress.slugOf(invitedTitle).equals(ArticleAddress.slugOf(address))) {
                model.addAttribute("invitation", invitedTitle);
            }
            return "error/404";
        }
        var article = found.get();

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
                        // FR-024: the moderator edits directly; anyone else proposes an edit (FR-011).
                        request.isUserInRole(MODERATOR)
                                ? "/moderate/articles/" + article.getSlug() + "/edit"
                                : ArticleAddress.pathOf(article.getSlug()) + "/edit",
                        ArticleAddress.pathOf(article.getSlug()) + "/report",
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
     * to proposing an edit (FR-011, {@code contribute}), or for the signed-in moderator to editing it
     * directly (FR-024, {@code moderate}), {@code reportPath} to reporting it (FR-021,
     * {@code report}). {@code removePath} leads to FR-026's removal
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
            String reportPath,
            String removePath) {}
}
