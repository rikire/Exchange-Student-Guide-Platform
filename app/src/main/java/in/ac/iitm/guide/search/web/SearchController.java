package in.ac.iitm.guide.search.web;

import in.ac.iitm.guide.search.internal.ArticleSearchService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@code GET /search?q=}: {@code 200} with the matches, none included. A missing or blank {@code q},
 * or one of more than {@link #MAX_WORDS} words, answers {@code 400} (routes.yml) with the same page
 * saying why.
 */
// trace:FR-007
@Controller
class SearchController {

    /**
     * Decided by the human, 28 Sep: a thousand words answered 500 once Lucene's 1024-clause limit
     * was passed (three fields per word), and no reader types fifty.
     */
    static final int MAX_WORDS = 50;

    private final ArticleSearchService search;

    SearchController(ArticleSearchService search) {
        this.search = search;
    }

    @GetMapping("/search")
    String search(@RequestParam(name = "q", required = false) String query, Model model, HttpServletResponse response) {
        if (query == null || query.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("query", "");
            return "search/SearchResults";
        }
        model.addAttribute("query", query);
        var words = search.wordsIn(query);
        // More than one word changes what an empty result means: none holds all of them (FR-007).
        model.addAttribute("words", words);
        if (words > MAX_WORDS) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("tooLong", true);
            return "search/SearchResults";
        }
        model.addAttribute("results", search.search(query));
        return "search/SearchResults";
    }
}
