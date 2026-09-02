package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommitMessageCheckTest {

    @TempDir
    Path tempDir;

    private List<String> check(String message) throws IOException {
        Path file = tempDir.resolve("COMMIT_EDITMSG");
        Files.writeString(file, message);
        return CommitMessageCheck.check(file);
    }

    @Test
    void accepts_a_feature_commit_that_names_its_requirement() throws IOException {
        assertEquals(List.of(), check("feat(FEAT-003): add article submission form [FR-012]"));
    }

    @Test
    void accepts_several_requirements_in_one_reference() throws IOException {
        assertEquals(List.of(), check("fix(FEAT-004): reject oversized uploads [FR-020, NFR-003]"));
    }

    @Test
    void accepts_a_docs_commit_without_scope_or_requirement() throws IOException {
        assertEquals(List.of(), check("docs: describe the moderation state machine"));
    }

    @Test
    void rejects_a_feature_commit_without_a_requirement_reference() throws IOException {
        assertTrue(check("feat(FEAT-003): add article submission form").get(0).contains("[FR-012]"));
    }

    @Test
    void rejects_a_feature_commit_without_a_feature_scope() throws IOException {
        assertTrue(check("feat: add article submission form [FR-012]").get(0).contains("FEAT-XXX"));
    }

    @Test
    void rejects_an_unknown_type() throws IOException {
        assertTrue(check("wip(FEAT-001): something [FR-001]").get(0).contains("unknown type"));
    }

    @Test
    void rejects_a_header_that_ends_with_a_period() throws IOException {
        assertTrue(check("docs: describe the moderation state machine.").get(0).contains("period"));
    }

    @Test
    void rejects_an_empty_message() throws IOException {
        assertTrue(check("# only comments here\n").get(0).contains("empty"));
    }

    @Test
    void ignores_comment_lines_when_locating_the_header() throws IOException {
        assertEquals(List.of(), check("# please enter the commit message\ndocs: add the glossary\n"));
    }

    @Test
    void rejects_a_debt_scope_that_is_not_numbered() throws IOException {
        assertFalse(check("refactor(DEBT): replace the rate limiter").isEmpty());
    }
}
