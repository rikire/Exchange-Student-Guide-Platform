package in.ac.iitm.guide.backup;

import java.util.List;

/**
 * What an import did: how many articles it created and the titles it left alone because an article
 * with that title already existed.
 */
// trace:NFR-004
public record ImportReport(int imported, List<String> skipped) {}
