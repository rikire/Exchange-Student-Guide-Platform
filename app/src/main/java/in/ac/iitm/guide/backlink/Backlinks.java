package in.ac.iitm.guide.backlink;

import in.ac.iitm.guide.backlink.persistence.LinkRepository;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "What links here" for an article page (FR-006): the live articles whose body links to it. */
// trace:FR-006
@Service
public class Backlinks {

    /** A page never lists more than this (ADR-0010's bounded reads). */
    public static final int LIMIT = 50;

    private final LinkRepository links;

    Backlinks(LinkRepository links) {
        this.links = links;
    }

    /** An article that links here, with the page to reach it. */
    public record Backlink(String title, String path) {}

    /** @return at most {@link #LIMIT} live articles linking to this one, by title; removed ones left out */
    @Transactional(readOnly = true)
    public List<Backlink> linkingTo(UUID articleId) {
        return links.findLiveSources(articleId, PageRequest.of(0, LIMIT)).stream()
                .map(source -> new Backlink(source.getTitle(), ArticleAddress.pathOf(source.getSlug())))
                .toList();
    }
}
