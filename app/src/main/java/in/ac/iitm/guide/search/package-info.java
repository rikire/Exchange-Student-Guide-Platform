/**
 * Indexing and querying. Hides its engine behind its own published type so other slices and their tests never need a Lucene index.
 *
 * <p>Serves FR-007.
 *
 * <p><strong>Declared, not yet built.</strong> This package holds only its module declaration: the
 * slice itself is written in phase 2 or 3 (see {@code docs/roadmap/}). It exists now so the
 * boundary described in {@code docs/ai/architecture-rules.md} is enforced by
 * {@code ModularityTest} rather than only described, and so Spring Modulith generates the module
 * canvas from the code instead of from a diagram someone drew.
 */
package in.ac.iitm.guide.search;
