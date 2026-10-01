package in.ac.iitm.guide.search.internal;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

/**
 * The analyzer FR-007's "independent of word form" rests on: "Registering" and "registration" reduce
 * to one stem. Built from Lucene's own factories (analysis-common, which the Lucene backend brings);
 * the backend's built-in analyzers do not stem. Named in application.yml.
 *
 * <p>NFR-003 rests on its tokenizer: Unicode's word rules split Devanagari and Tamil into words with
 * their signs, and the filters after it leave non-Latin letters as they are, so a Hindi or Tamil word
 * is found as written. Their other word forms are not: the stemmer is English.
 */
// trace:FR-007
// trace:NFR-003
public class EnglishAnalysis implements LuceneAnalysisConfigurer {

    static final String ENGLISH = "english";

    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer(ENGLISH)
                .custom()
                .tokenizer(StandardTokenizerFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class)
                .tokenFilter(ASCIIFoldingFilterFactory.class)
                .tokenFilter(SnowballPorterFilterFactory.class)
                .param("language", "English");
    }
}
