package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Tag;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;

/**
 * What is indexed, written here rather than as annotations on the shared entity so the engine stays
 * inside this slice. Only {@link Article} is indexed: a submission is never in the index, which is
 * what keeps pending and rejected ones out of the results (FEAT-007). Named in application.yml.
 */
// trace:FR-007
public class ArticleSearchMapping implements HibernateOrmSearchMappingConfigurer {

    static final String TITLE = "title";
    static final String BODY = "body";
    static final String TAG_NAMES = "tags.name";
    static final String REMOVED_AT = "removedAt";

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        var mapping = context.programmaticMapping();

        var article = mapping.type(Article.class);
        article.indexed();
        article.property(TITLE).fullTextField().analyzer(EnglishAnalysis.ENGLISH);
        article.property(BODY).fullTextField().analyzer(EnglishAnalysis.ENGLISH);
        article.property(REMOVED_AT).genericField();
        // A tag's name never changes once stored (ADR-0005), and Tag has no link back to its
        // articles, so only a change to an article's own tag set re-indexes it.
        article.property("tags")
                .indexedEmbedded()
                .includePaths("name")
                .indexingDependency()
                .reindexOnUpdate(ReindexOnUpdate.SHALLOW);

        mapping.type(Tag.class).property("name").fullTextField().analyzer(EnglishAnalysis.ENGLISH);
    }
}
