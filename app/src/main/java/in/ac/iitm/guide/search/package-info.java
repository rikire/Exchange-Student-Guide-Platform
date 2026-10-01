/**
 * Indexing and querying, {@code GET /search?q=}: published articles matching any word of the query
 * in their title, body or tags, fuller matches first (FEAT-007).
 *
 * <p>Serves FR-007. The package root publishes {@link in.ac.iitm.guide.search.SimilarTitles}, the
 * not-found page's "Did you mean" (fix 3.8), and nothing else. The
 * engine (Hibernate Search over Lucene, ADR-0004) stays inside: the index mapping and the analyzer
 * are written in {@code internal}, not as annotations on the shared entities.
 *
 * <p>Hindi and Tamil are searched as written (NFR-003); their word forms are not, the stemmer being
 * English. <strong>Not built yet:</strong> paging.
 */
package in.ac.iitm.guide.search;
