package in.ac.iitm.guide.tools;

import static in.ac.iitm.guide.tools.CommandRules.Verdict.Decision.ALLOW;
import static in.ac.iitm.guide.tools.CommandRules.Verdict.Decision.ASK;
import static in.ac.iitm.guide.tools.CommandRules.Verdict.Decision.DENY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandRulesTest {

    private static CommandRules.Verdict.Decision command(String text) {
        return CommandRules.forCommand(text).decision();
    }

    private static CommandRules.Verdict.Decision content(String path, String text) {
        return CommandRules.forContent(path, text).decision();
    }

    @Test
    void skipping_the_commit_hooks_is_refused() {
        assertEquals(DENY, command("git commit --no-verify -m \"wip\""));
    }

    @Test
    void skipping_the_tests_is_refused() {
        assertEquals(DENY, command("./mvnw -DskipTests package"));
        assertEquals(DENY, command("./mvnw -Dmaven.test.skip=true package"));
    }

    @Test
    void writing_about_a_bypass_flag_is_not_using_one() {
        // This rule blocked the very command that was documenting it: the text being written
        // mentioned --no-verify, and an earlier version could not tell that from an invocation.
        // A check that cannot make that distinction is one people switch off.
        assertEquals(
                ALLOW, command("python -c \"open('doc.md','w').write('Refuses `--no-verify` and `-DskipTests`')\""));
        assertEquals(ALLOW, command("grep -n 'no-verify' docs/repository-map.md"));
    }

    @Test
    void a_bypass_flag_in_a_neighbouring_segment_does_not_condemn_the_whole_line() {
        assertEquals(ALLOW, command("echo '--no-verify is forbidden' && git status"));
    }

    @Test
    void a_flag_that_switches_the_tests_back_on_is_not_a_bypass() {
        // This refused `./mvnw package -DskipTests=false` within an hour of the rule being written.
        // The command asks for the tests; the substring made it look like it was dodging them.
        assertEquals(ALLOW, command("./mvnw -B -pl tools package -DskipTests=false"));
        assertEquals(ALLOW, command("./mvnw package -Dmaven.test.skip=false"));
        assertEquals(DENY, command("./mvnw package -DskipTests=true"));
    }

    @Test
    void an_ordinary_command_runs() {
        assertEquals(ALLOW, command("git status --porcelain"));
        assertEquals(ALLOW, command("./mvnw -B verify"));
    }

    @Test
    void a_build_piped_into_another_command_is_questioned() {
        // The shell reports tail's exit code, and tail all but always succeeds. This exact shape
        // printed "BUILD OK" over a broken compile twice in this repository's first two days.
        assertEquals(ASK, command("./mvnw -q -B package 2>&1 | tail -5"));
        assertEquals(ASK, command("scripts/check.sh | grep -E 'Tests run'"));
    }

    @Test
    void the_same_pipeline_runs_when_the_real_exit_code_is_read() {
        assertEquals(ALLOW, command("./mvnw -B package | tail -5; echo ${PIPESTATUS[0]}"));
        assertEquals(ALLOW, command("set -o pipefail; ./mvnw -B verify | tail -3"));
    }

    @Test
    void piping_something_that_is_not_a_build_is_not_questioned() {
        assertEquals(ALLOW, command("git log --oneline | head -5"));
        assertEquals(ALLOW, command("cat docs/README.md | grep architecture"));
    }

    @Test
    void a_deferred_work_marker_without_a_debt_entry_is_refused() {
        assertEquals(DENY, content("app/src/main/java/Thing.java", "// TODO: come back to this\n"));
        assertEquals(DENY, content("scripts/run.sh", "# FIXME make this portable\n"));
    }

    @Test
    void a_marker_that_names_its_debt_entry_is_allowed() {
        assertEquals(ALLOW, content("app/src/main/java/Thing.java", "// TODO(DEBT-007): replace the stub\n"));
    }

    @Test
    void the_debt_register_and_the_instructions_may_describe_markers() {
        // They quote the forms rather than defer work, and blocking them would make the rule
        // impossible to document.
        assertEquals(ALLOW, content("docs/tech-debt.md", "Use `// TODO(DEBT-007): ...` in the code.\n"));
        assertEquals(ALLOW, content("docs/ai/workflow.md", "Never leave a bare TODO in the code.\n"));
    }

    @Test
    void a_word_that_merely_contains_the_letters_is_not_a_marker() {
        assertEquals(ALLOW, content("docs/notes.md", "The autodetect step runs first.\n"));
    }

    @Test
    void the_refusal_says_which_line_caused_it() {
        String reason = CommandRules.forContent("A.java", "int x = 1;\n// HACK: works for now\n")
                .reason();

        assertTrue(reason.contains("HACK: works for now"), "a refusal that does not point at the line is a riddle");
    }
}
