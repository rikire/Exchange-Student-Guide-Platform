package in.ac.iitm.guide.taxonomy.web;

import in.ac.iitm.guide.taxonomy.internal.TagBrowseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** {@code GET /tags/{tag}}: the published articles carrying a tag, most recently updated first. */
// trace:FR-008
@Controller
class TagController {

    private final TagBrowseService tags;

    TagController(TagBrowseService tags) {
        this.tags = tags;
    }

    @GetMapping("/tags/{tag}")
    String browse(@PathVariable String tag, Model model) {
        model.addAttribute("tagPage", tags.browse(tag).orElseThrow(() -> new TagNotFoundException(tag)));
        return "taxonomy/TagBrowse";
    }
}
