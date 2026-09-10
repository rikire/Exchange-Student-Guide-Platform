/**
 * The queue, approval and rejection, plus the revision retained when an approved edit changes an article.
 *
 * <p>Serves FR-014 to FR-020, FR-026.
 *
 * <p><strong>Declared, not yet built.</strong> This package holds only its module declaration: the
 * slice itself is written in phase 2 or 3 (see {@code docs/roadmap/}). It exists now so the
 * boundary described in {@code docs/ai/architecture-rules.md} is enforced by
 * {@code ModularityTest} rather than only described, and so Spring Modulith generates the module
 * canvas from the code instead of from a diagram someone drew.
 */
package in.ac.iitm.guide.moderate;
