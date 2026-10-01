package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-007's query. Every word must match somewhere in the article — its title, its body or a tag — and
 * Lucene's scoring orders the articles that hold them all (changed by the human on 2 Oct, fix 1.5).
 * Each word is analysed once, here, and searched as the term the index holds. Transactional so the
 * tags of the hits load inside the session, in batches, before the view renders.
 */
// trace:FR-007
@Service
@Transactional(readOnly = true)
public class ArticleSearchService {

    // No paging yet (FEAT-007, out of scope): the first results and the total are shown.
    // TODO(DEBT-021): no test holds this bound, nor the page's query count.
    static final int LIMIT = 20;

    private final EntityManager entityManager;

    ArticleSearchService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Characters of the body a result quotes, around its first matched word. */
    static final int PASSAGE = 180;

    /** More characters than a title may hold. */
    private static final int TITLE = 300;

    public SearchResults search(String query) {
        var terms = termsOf(query);
        if (terms.isEmpty()) {
            return new SearchResults(query, 0, List.of());
        }
        var result = Search.session(entityManager)
                .search(Article.class)
                .select(f -> f.composite()
                        .from(
                                f.entity(),
                                f.highlight(ArticleSearchMapping.TITLE).highlighter("title"),
                                f.highlight(ArticleSearchMapping.BODY)
                                        .highlighter("passage")
                                        .single())
                        .as(Found::new))
                .where(f -> {
                    var every = f.bool().mustNot(f.exists().field(ArticleSearchMapping.REMOVED_AT));
                    for (var term : terms) {
                        every = every.must(f.match()
                                .fields(
                                        ArticleSearchMapping.TITLE,
                                        ArticleSearchMapping.BODY,
                                        ArticleSearchMapping.TAG_NAMES)
                                .matching(term)
                                .skipAnalysis());
                    }
                    return every;
                })
                // The plain highlighter, since Lucene's unified one takes no passage length.
                .highlighter("passage", f -> f.plain()
                        .tag(Passage.OPEN, Passage.CLOSE)
                        .encoder(HighlighterEncoder.DEFAULT)
                        .fragmentSize(PASSAGE)
                        .numberOfFragments(1))
                // Longer than any title (255), so a title comes back whole.
                .highlighter("title", f -> f.plain()
                        .tag(Passage.OPEN, Passage.CLOSE)
                        .encoder(HighlighterEncoder.DEFAULT)
                        .fragmentSize(TITLE)
                        .numberOfFragments(1))
                .fetch(LIMIT);

        var hits = result.hits().stream().map(ArticleSearchService::hit).toList();
        return new SearchResults(query, result.total().hitCount(), hits);
    }

    /** One hit as the query returns it: the article, its title marked, and its body's passage. */
    private record Found(Article article, List<String> title, String passage) {}

    /**
     * How many words the query holds as the index's analyzer splits it — at hyphens, commas and
     * between ideographs as well as at spaces — since each becomes a clause per field and Lucene
     * refuses a query of more than 1024.
     */
    public int wordsIn(String query) {
        return termsOf(query).size();
    }

    /** The query's words as the index's analyzer reduces them: lower case, folded, stemmed. */
    private List<String> termsOf(String query) {
        var analyzer = Search.mapping(entityManager.getEntityManagerFactory())
                .backend()
                .unwrap(LuceneBackend.class)
                .analyzer(EnglishAnalysis.ENGLISH)
                .orElseThrow();
        var terms = new ArrayList<String>();
        try (var tokens = analyzer.tokenStream(ArticleSearchMapping.BODY, query)) {
            var term = tokens.addAttribute(CharTermAttribute.class);
            tokens.reset();
            while (tokens.incrementToken()) {
                terms.add(term.toString());
            }
            tokens.end();
        } catch (IOException e) {
            // Lucene reads the query from a String here; there is no I/O to fail.
            throw new UncheckedIOException(e);
        }
        return terms;
    }

    private static SearchResults.Hit hit(Found found) {
        var article = found.article();
        var tags = TagLink.of(article.getTags().stream().map(Tag::getName).toList());
        var title = found.title().isEmpty()
                ? Passage.plain(article.getTitle())
                : Passage.of(found.title().get(0));
        var passage = found.passage() == null ? Passage.none() : Passage.of(found.passage());
        return new SearchResults.Hit(
                title, article.getSummary(), passage, ArticleAddress.pathOf(article.getSlug()), tags);
    }
}
