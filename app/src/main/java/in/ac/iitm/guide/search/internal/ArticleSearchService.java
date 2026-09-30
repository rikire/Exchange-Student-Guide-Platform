package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.TagLink;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-007's query. Any of the words may match, in the title, the body or a tag; Lucene's scoring puts
 * the articles matching more of them first. Transactional so the tags of the hits load inside the
 * session, in batches, before the view renders.
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

    public SearchResults search(String query) {
        var result = Search.session(entityManager)
                .search(Article.class)
                .where(f -> f.bool()
                        .must(f.match()
                                .fields(
                                        ArticleSearchMapping.TITLE,
                                        ArticleSearchMapping.BODY,
                                        ArticleSearchMapping.TAG_NAMES)
                                .matching(query))
                        .mustNot(f.exists().field(ArticleSearchMapping.REMOVED_AT)))
                .fetch(LIMIT);

        var hits = result.hits().stream().map(ArticleSearchService::hit).toList();
        return new SearchResults(query, result.total().hitCount(), hits);
    }

    /**
     * How many words the query holds as the index's analyzer splits it — at hyphens, commas and
     * between ideographs as well as at spaces — since each becomes a clause per field and Lucene
     * refuses a query of more than 1024.
     */
    public int wordsIn(String query) {
        var analyzer = Search.mapping(entityManager.getEntityManagerFactory())
                .backend()
                .unwrap(LuceneBackend.class)
                .analyzer(EnglishAnalysis.ENGLISH)
                .orElseThrow();
        var words = 0;
        try (var tokens = analyzer.tokenStream(ArticleSearchMapping.BODY, query)) {
            tokens.reset();
            while (tokens.incrementToken()) {
                words++;
            }
            tokens.end();
        } catch (IOException e) {
            // Lucene reads the query from a String here; there is no I/O to fail.
            throw new UncheckedIOException(e);
        }
        return words;
    }

    private static SearchResults.Hit hit(Article article) {
        var tags = TagLink.of(article.getTags().stream().map(Tag::getName).toList());
        return new SearchResults.Hit(
                article.getTitle(), article.getSummary(), ArticleAddress.pathOf(article.getSlug()), tags);
    }
}
