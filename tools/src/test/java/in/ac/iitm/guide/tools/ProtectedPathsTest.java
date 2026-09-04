package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProtectedPathsTest {

    @Test
    void requirements_are_protected() {
        assertNotNull(ProtectedPaths.reasonFor("docs/requirements/functional.md"));
    }

    @Test
    void migrations_are_protected() {
        assertNotNull(ProtectedPaths.reasonFor("app/src/main/resources/db/migration/V1__initial.sql"));
    }

    @Test
    void the_assistants_own_instructions_are_protected() {
        assertNotNull(ProtectedPaths.reasonFor("CLAUDE.md"));
        assertNotNull(ProtectedPaths.reasonFor("docs/ai/workflow.md"));
        assertNotNull(ProtectedPaths.reasonFor(".claude/settings.json"));
    }

    @Test
    void the_journal_is_written_by_the_hooks_and_must_not_ask() {
        assertNull(ProtectedPaths.reasonFor("docs/ai/journal/2026-09-02-abcd1234.md"));
    }

    @Test
    void the_journals_own_instructions_are_not_an_entry() {
        // The exemption above is for what the hooks write. It was also exempting the README beside
        // them, which is an instruction file and was being edited without anyone being asked.
        assertNotNull(ProtectedPaths.reasonFor("docs/ai/journal/README.md"));
    }

    @Test
    void feature_files_and_the_debt_register_are_kept_by_the_assistant_itself() {
        assertNull(ProtectedPaths.reasonFor("docs/features/FEAT-001-article-view.md"));
        assertNull(ProtectedPaths.reasonFor("docs/tech-debt.md"));
    }

    @Test
    void ordinary_source_files_are_not_protected() {
        assertNull(ProtectedPaths.reasonFor("app/src/main/java/in/ac/iitm/guide/GuideApplication.java"));
    }

    @Test
    void a_path_outside_the_repository_is_not_protected() {
        assertNull(ProtectedPaths.reasonFor(null));
    }
}
