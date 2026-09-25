package in.ac.iitm.guide.articleview.web;

import in.ac.iitm.guide.articleview.persistence.ArticleReadRepository;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
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
@Controller
class ArticleController {

    private final ArticleReadRepository articles;

    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    ArticleController(ArticleReadRepository articles) {
        this.articles = articles;
    }

    @GetMapping("/articles/{address}")
    String show(@PathVariable String address, Model model) {
        // The address is put through the same rule as a title, so /articles/HOSTEL-Life reaches the
        // article stored under "hostel-life" (ui-routes.md: matched case-insensitively).
        var slug = ArticleAddress.slugOf(address).orElseThrow(() -> new ArticleNotFoundException(address));
        var article =
                articles.findBySlugAndRemovedAtIsNull(slug).orElseThrow(() -> new ArticleNotFoundException(address));

        var tags = article.getTags().stream().map(Tag::getName).sorted().toList();
        model.addAttribute(
                "article",
                new ArticlePage(article.getTitle(), tags, renderer.render(article.getBody(), this::resolve)));
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

    /** What the template shows; {@code bodyHtml} is the converter's output and is the only unescaped part. */
    record ArticlePage(String title, List<String> tags, String bodyHtml) {}
}
