/**
 * Export and import of the whole knowledge base, in the format the seed content also uses: one
 * Markdown file per article with YAML front matter (ADR-0007).
 *
 * <p>Serves NFR-004. Published: {@link in.ac.iitm.guide.backup.ArticleArchive}, with its
 * {@link in.ac.iitm.guide.backup.ImportReport} and {@link in.ac.iitm.guide.backup.ArchiveFormatException}.
 *
 * <p><strong>Not built yet:</strong> media travelling with the articles, and any way for the admin to
 * start an export — that waits for the admin panel in phase 4. Today an export is called from code, and an import runs from the
 * {@code seed} profile.
 */
package in.ac.iitm.guide.backup;
