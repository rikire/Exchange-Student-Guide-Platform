/**
 * Indexing and querying, {@code GET /search?q=}: published articles matching any word of the query
 * in their title, body or tags, fuller matches first (FEAT-007).
 *
 * <p>Serves FR-007. The package root publishes {@link in.ac.iitm.guide.search.SimilarTitles}, the
 * not-found page's "Did you mean" (fix 3.8), and nothing else. The
 * engine (Hibernate Search over Lucene, ADR-0004) stays inside: the index mapping and the analyzer
 * are written in {@code internal}, not as annotations on the shared entities.
 *
 * <p><strong>Not built yet:</strong> Hindi and Tamil queries (NFR-003), the latency bound
 * (NFR-002), paging and highlighted snippets.
 */
package in.ac.iitm.guide.search;
