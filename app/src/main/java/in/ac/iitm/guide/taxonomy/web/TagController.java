package in.ac.iitm.guide.taxonomy.web;

import in.ac.iitm.guide.taxonomy.internal.TagBrowseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * {@code GET /tags}: every tag of a live article by name (fix 2.1). {@code GET /tags/{tag}}: the
 * published articles carrying a tag, most recently updated first. A page
 * that answers counts one visit to its tags (FR-031, ADR-0017).
 */
// trace:FR-008
// trace:FR-031
@Controller
class TagController {

    private final TagBrowseService tags;

    TagController(TagBrowseService tags) {
        this.tags = tags;
    }

    @GetMapping("/tags")
    String index(Model model) {
        model.addAttribute("index", tags.index());
        return "taxonomy/TagIndex";
    }

    @GetMapping("/tags/{tag}")
    String browse(@PathVariable String tag, Model model) {
        var page = tags.browse(tag).orElseThrow(() -> new TagNotFoundException(tag));
        tags.countVisit(page);
        model.addAttribute("tagPage", page);
        return "taxonomy/TagBrowse";
    }
}
